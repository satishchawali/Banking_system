package service;

import database.DatabaseConnection;
import model.AccountType;
import model.BankTransaction;
import model.TransactionType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** End-to-end JDBC checks. Enable explicitly against a disposable local test schema. */
@EnabledIfEnvironmentVariable(named = "BANKING_INTEGRATION_TESTS", matches = "true")
class BankingServiceIntegrationTest {
    private final BankingService bank = new BankingService();

    @Test
    void persistsAndAuthorizesAccountTransactionsAndPassbook() throws Exception {
        String pin = "4826";
        String source = bank.createAccount("Integration Source", "Test Street", "9876543210",
                AccountType.SAVINGS, new BigDecimal("1000.00"), pin);
        String recipient = bank.createAccount("Integration Recipient", "Test Street", "9876543211",
                AccountType.CURRENT, BigDecimal.ZERO, pin);

        try {
            BankTransaction deposit = bank.deposit(source, new BigDecimal("250.00"), pin);
            assertEquals(TransactionType.DEPOSIT, deposit.type());
            assertEquals(new BigDecimal("1250.00"), deposit.balanceAfter());

            BankTransaction withdrawal = bank.withdraw(source, new BigDecimal("100.00"), pin);
            assertEquals(new BigDecimal("1150.00"), withdrawal.balanceAfter());

            String reference = bank.transfer(source, recipient, new BigDecimal("200.00"), pin);
            assertNotNull(reference);
            assertEquals(new BigDecimal("950.00"), bank.balance(source, pin));
            assertEquals(new BigDecimal("200.00"), bank.balance(recipient, pin));

            assertThrows(IllegalArgumentException.class,
                    () -> bank.transfer(source, recipient, new BigDecimal("1000.00"), pin));
            assertEquals(new BigDecimal("950.00"), bank.balance(source, pin), "failed transfer must not debit");
            assertThrows(SecurityException.class, () -> bank.balance(source, "0000"));

            LocalDate today = LocalDate.now();
            List<BankTransaction> history = bank.history(source, pin, today, today);
            assertEquals(4, history.size(), "opening, deposit, withdrawal and transfer-out are persisted");
            assertEquals(new BigDecimal("950.00"), history.get(history.size() - 1).balanceAfter());

            PassbookStatement statement = new PassbookService(bank).statement(source, pin, today, today);
            assertEquals(BigDecimal.ZERO, statement.openingBalance());
            assertEquals(new BigDecimal("950.00"), statement.closingBalance());
            assertEquals(history.size(), statement.transactions().size());
        } finally {
            deleteTestAccounts(source, recipient);
        }
    }

    private void deleteTestAccounts(String... accountNumbers) throws Exception {
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                for (String accountNumber : accountNumbers) {
                    long customerId;
                    try (PreparedStatement findCustomer = connection.prepareStatement(
                            "SELECT customer_id FROM accounts WHERE account_number=?")) {
                        findCustomer.setString(1, accountNumber);
                        try (var result = findCustomer.executeQuery()) {
                            if (!result.next()) continue;
                            customerId = result.getLong(1);
                        }
                    }
                    try (PreparedStatement deleteTransactions = connection.prepareStatement(
                            "DELETE FROM transactions WHERE account_number=?")) {
                        deleteTransactions.setString(1, accountNumber);
                        deleteTransactions.executeUpdate();
                    }
                    try (PreparedStatement deleteAccount = connection.prepareStatement(
                            "DELETE FROM accounts WHERE account_number=?")) {
                        deleteAccount.setString(1, accountNumber);
                        deleteAccount.executeUpdate();
                    }
                    try (PreparedStatement deleteCustomer = connection.prepareStatement(
                            "DELETE FROM customers WHERE customer_id=? AND NOT EXISTS " +
                                    "(SELECT 1 FROM accounts WHERE customer_id=?)")) {
                        deleteCustomer.setLong(1, customerId);
                        deleteCustomer.setLong(2, customerId);
                        deleteCustomer.executeUpdate();
                    }
                }
                connection.commit();
            } catch (Exception failure) {
                connection.rollback();
                throw failure;
            }
        }
    }
}
