package model;
import interfaces.Authenticatable;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.mindrot.jbcrypt.BCrypt;
public final class CurrentAccount extends Account implements Authenticatable {
 private final String pinHash;
 public CurrentAccount(String n,long c,BigDecimal b,LocalDate d,AccountStatus s,BigDecimal limit,String pinHash){super(n,c,b,d,s,limit);this.pinHash=pinHash;}
 public AccountType getType(){return AccountType.CURRENT;} public BigDecimal minimumOpeningDeposit(){return new BigDecimal("1000.00");}
 public boolean verifyPin(String pin){return pin!=null&&pinHash!=null&&BCrypt.checkpw(pin,pinHash);}
}
