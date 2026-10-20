package service;
import model.*;import org.junit.jupiter.api.Test;import java.math.BigDecimal;import java.time.*;import java.util.List;import static org.junit.jupiter.api.Assertions.*;
class WithdrawalPolicyTest {
 private final LocalDate today=LocalDate.of(2026,10,20);private BankTransaction tx(String id,TransactionType type,String amount,LocalDate date){return new BankTransaction(id,"1234567890",type,new BigDecimal(amount),date.atStartOfDay(),BigDecimal.ZERO,null);}
 @Test void sumsSuccessfulWithdrawalsAndTransferOutsForBankingDate(){var ledger=List.of(tx("1",TransactionType.WITHDRAWAL,"7000.00",today),tx("2",TransactionType.TRANSFER_OUT,"6000.00",today),tx("3",TransactionType.DEPOSIT,"9000.00",today),tx("4",TransactionType.WITHDRAWAL,"1000.00",today.minusDays(1)));assertEquals(new BigDecimal("13000.00"),WithdrawalPolicy.spentOnDate(ledger,today));}
 @Test void allowsMultipleWithdrawalsUpToDailyLimit(){BigDecimal spent=new BigDecimal("12500.00"),limit=new BigDecimal("20000.00");assertEquals(new BigDecimal("7500.00"),WithdrawalPolicy.remaining(limit,spent));assertDoesNotThrow(()->WithdrawalPolicy.requireWithinLimit(limit,spent,new BigDecimal("7500.00")));}
 @Test void rejectsAmountAboveRemainingAllowance(){assertThrows(IllegalArgumentException.class,()->WithdrawalPolicy.requireWithinLimit(new BigDecimal("20000.00"),new BigDecimal("15000.00"),new BigDecimal("5000.01")));}
 @Test void nextBankingDateStartsWithNoWithdrawals(){var ledger=List.of(tx("1",TransactionType.WITHDRAWAL,"20000.00",today));assertEquals(BigDecimal.ZERO,WithdrawalPolicy.spentOnDate(ledger,today.plusDays(1)));assertEquals(new BigDecimal("20000.00"),WithdrawalPolicy.remaining(new BigDecimal("20000.00"),BigDecimal.ZERO));}
}
