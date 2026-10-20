package model;
import java.math.BigDecimal;import java.time.LocalDateTime;
public record BankTransaction(String id,String accountNumber,TransactionType type,BigDecimal amount,LocalDateTime timestamp,BigDecimal balanceAfter,String reference) {}
