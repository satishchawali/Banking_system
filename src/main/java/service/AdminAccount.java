package service;
import model.AccountStatus;import model.AccountType;import java.math.BigDecimal;import java.time.LocalDate;
public record AdminAccount(String accountNumber,String customerName,String mobile,AccountType type,BigDecimal balance,AccountStatus status,LocalDate openingDate){}
