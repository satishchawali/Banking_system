package model;
import interfaces.Authenticatable;
import interfaces.InterestBearing;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.mindrot.jbcrypt.BCrypt;
public final class SavingsAccount extends Account implements InterestBearing, Authenticatable {
 private final String pinHash;
 public SavingsAccount(String n,long c,BigDecimal b,LocalDate d,AccountStatus s,BigDecimal limit,String pinHash){super(n,c,b,d,s,limit);this.pinHash=pinHash;}
 public AccountType getType(){return AccountType.SAVINGS;} public BigDecimal minimumOpeningDeposit(){return new BigDecimal("500.00");}
 public BigDecimal calculateAnnualInterest(BigDecimal rate){if(rate==null||rate.signum()<0)throw new IllegalArgumentException("Invalid interest rate.");return getBalance().multiply(rate).setScale(2,java.math.RoundingMode.HALF_UP);}
 public boolean verifyPin(String pin){return pin!=null&&pinHash!=null&&BCrypt.checkpw(pin,pinHash);}
}
