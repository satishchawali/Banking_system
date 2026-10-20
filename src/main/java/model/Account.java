package model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

public abstract class Account {
    private final String accountNumber; private final long customerId; private BigDecimal balance; private final LocalDate openingDate; private AccountStatus status; private final BigDecimal dailyLimit;
    protected Account(String number,long customerId,BigDecimal balance,LocalDate openingDate,AccountStatus status,BigDecimal dailyLimit){
        if(number==null||!number.matches("[0-9]{10,20}"))throw new IllegalArgumentException("Invalid account number."); if(customerId<=0)throw new IllegalArgumentException("Invalid customer.");
        this.accountNumber=number;this.customerId=customerId;this.balance=money(balance);this.openingDate=Objects.requireNonNull(openingDate);this.status=Objects.requireNonNull(status);this.dailyLimit=money(dailyLimit);
    }
    public abstract AccountType getType(); public abstract BigDecimal minimumOpeningDeposit();
    public String getAccountNumber(){return accountNumber;} public long getCustomerId(){return customerId;} public BigDecimal getBalance(){return balance;} public LocalDate getOpeningDate(){return openingDate;} public AccountStatus getStatus(){return status;} public BigDecimal getDailyLimit(){return dailyLimit;}
    public void deposit(BigDecimal amount){ensureActive();amount=positive(amount);balance=balance.add(amount);}
    public void withdraw(BigDecimal amount){ensureActive();amount=positive(amount);if(balance.compareTo(amount)<0)throw new IllegalArgumentException("Insufficient balance.");balance=balance.subtract(amount);}
    public void close(){ensureActive();if(balance.signum()!=0)throw new IllegalStateException("Account balance must be zero before closure.");status=AccountStatus.CLOSED;}
    public void ensureActive(){if(status!=AccountStatus.ACTIVE)throw new IllegalStateException("Account is closed.");}
    public static BigDecimal money(BigDecimal v){if(v==null||v.scale()>2)throw new IllegalArgumentException("Amount must have at most two decimal places.");return v.setScale(2);}
    public static BigDecimal positive(BigDecimal v){v=money(v);if(v.signum()<=0)throw new IllegalArgumentException("Amount must be greater than zero.");return v;}
}
