package service;
import interfaces.Printable;import model.BankTransaction;import model.TransactionType;import java.math.BigDecimal;import java.time.LocalDate;import java.util.List;
/** Immutable, saved-ledger-backed statement with computed period opening and closing balances. */
public record PassbookStatement(String accountNumber,LocalDate from,LocalDate to,BigDecimal openingBalance,BigDecimal closingBalance,List<BankTransaction> transactions) implements Printable {
 public PassbookStatement { transactions=List.copyOf(transactions); }
 public String render(){StringBuilder s=new StringBuilder("Passbook: ").append(accountNumber).append(" | ").append(from).append(" to ").append(to).append("\nOpening: ₹").append(openingBalance).append(" | Closing: ₹").append(closingBalance).append("\n");for(BankTransaction t:transactions)s.append(t.id()).append(" | ").append(t.timestamp()).append(" | ").append(t.type()).append(" | ₹").append(t.amount()).append(" | ₹").append(t.balanceAfter()).append('\n');return s.toString();}
 public static BigDecimal signedChange(BankTransaction t){return switch(t.type()){case OPENING,DEPOSIT,TRANSFER_IN,INTEREST->t.amount();case WITHDRAWAL,TRANSFER_OUT->t.amount().negate();};}
}
