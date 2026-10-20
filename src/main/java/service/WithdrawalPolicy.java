package service;
import model.BankTransaction;import model.TransactionType;import java.math.BigDecimal;import java.time.LocalDate;import java.util.Collection;
/** Pure policy for the per-banking-date withdrawal ceiling. Failed attempts never appear in the saved ledger input. */
public final class WithdrawalPolicy {
 private WithdrawalPolicy(){}
 public static BigDecimal spentOnDate(Collection<BankTransaction> ledger,LocalDate date){return ledger.stream().filter(t->t.timestamp().toLocalDate().equals(date)).filter(t->t.type()==TransactionType.WITHDRAWAL||t.type()==TransactionType.TRANSFER_OUT).map(BankTransaction::amount).reduce(BigDecimal.ZERO,BigDecimal::add);}
 public static BigDecimal remaining(BigDecimal limit,BigDecimal spent){if(limit==null||spent==null||limit.signum()<0||spent.signum()<0)throw new IllegalArgumentException("Invalid withdrawal totals.");return limit.subtract(spent).max(BigDecimal.ZERO);}
 public static void requireWithinLimit(BigDecimal limit,BigDecimal spent,BigDecimal amount){if(amount.signum()<=0)throw new IllegalArgumentException("Amount must be positive.");BigDecimal left=remaining(limit,spent);if(amount.compareTo(left)>0)throw new IllegalArgumentException("Daily withdrawal limit exceeded. Remaining: "+left.setScale(2));}
}
