package service;
import model.*;import org.junit.jupiter.api.Test;import java.math.BigDecimal;import java.time.LocalDate;import static org.junit.jupiter.api.Assertions.*;
class AccountModelTest {
 private final BigDecimal limit=new BigDecimal("20000.00");
 @Test void savingsAndCurrentUsePolymorphicAccountRules(){Account s=new SavingsAccount("1234567890",1,BigDecimal.ZERO,LocalDate.now(),AccountStatus.ACTIVE,limit,"unused");Account c=new CurrentAccount("1234567891",2,BigDecimal.ZERO,LocalDate.now(),AccountStatus.ACTIVE,limit,"unused");assertEquals(AccountType.SAVINGS,s.getType());assertEquals(AccountType.CURRENT,c.getType());assertEquals(new BigDecimal("500.00"),s.minimumOpeningDeposit());assertEquals(new BigDecimal("1000.00"),c.minimumOpeningDeposit());}
 @Test void rejectsInvalidAmountsAndOverdraft(){Account a=new CurrentAccount("1234567891",2,new BigDecimal("10.00"),LocalDate.now(),AccountStatus.ACTIVE,limit,"unused");assertThrows(IllegalArgumentException.class,()->a.deposit(BigDecimal.ZERO));assertThrows(IllegalArgumentException.class,()->a.withdraw(new BigDecimal("11.00")));assertEquals(new BigDecimal("10.00"),a.getBalance());}
 @Test void savingsInterestRoundsToCents(){SavingsAccount a=new SavingsAccount("1234567890",1,new BigDecimal("100.00"),LocalDate.now(),AccountStatus.ACTIVE,limit,"unused");assertEquals(new BigDecimal("4.00"),a.calculateAnnualInterest(new BigDecimal("0.04")));}
 @Test void rejectsBadCustomerAndClosedTransactions(){assertThrows(IllegalArgumentException.class,()->new Customer(1,"","address","1234567","",java.time.LocalDateTime.now()));Account a=new CurrentAccount("1234567891",2,BigDecimal.ZERO,LocalDate.now(),AccountStatus.CLOSED,limit,"unused");assertThrows(IllegalStateException.class,()->a.deposit(BigDecimal.ONE));}
}
