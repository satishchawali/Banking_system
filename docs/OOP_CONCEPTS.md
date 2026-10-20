# OOP concepts

- **Encapsulation:** account identity and balances are private; deposit/withdraw/close methods control state. Customer fields are private with validated updates.
- **Abstraction and inheritance:** abstract `Account` defines common identity, balance and status; `SavingsAccount` and `CurrentAccount` specialize it.
- **Polymorphism:** account type and minimum deposit are overridden; `InterestBearing` is implemented only by SavingsAccount.
- **Interfaces:** Authenticatable provides PIN verification and InterestBearing declares interest calculation.
- **Composition:** BankingService uses JDBC connections and transaction records; a customer owns accounts via database foreign keys.
- **Enums, records, generics:** account/transaction states are enums, BankTransaction is a record and ledger results use `List<BankTransaction>`.
- **Other:** constructor overloads, prepared statements, try-with-resources, BigDecimal, Clock injection, exceptions and database transactions are used in the implementation.
