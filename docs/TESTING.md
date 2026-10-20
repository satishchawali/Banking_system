# Testing

Run `mvn test` with JDK 17+ and Maven installed. The unit suite covers account model behavior, passbook rendering/PDF generation, and daily withdrawal policy/date reset calculations. A JDBC integration test is enabled only when `BANKING_INTEGRATION_TESTS=true`; it creates disposable accounts, verifies PIN authorization, deposit, withdrawal, transfer, failed-transfer integrity and date-filtered passbook balances, then removes its rows. Run it against a dedicated, disposable schema after applying `database/schema.sql`:

```powershell
$env:DB_URL='jdbc:mysql://localhost:3306/banking_db?serverTimezone=Asia/Kolkata'
$env:DB_USER='root'
$env:DB_PASSWORD='your-local-password'
$env:BANKING_INTEGRATION_TESTS='true'
mvn test
```

The integration test cleanup is scoped to the exact accounts it creates. Do not point integration tests at a shared or production database.
