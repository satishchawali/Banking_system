# Banking Management System

A Java Swing banking simulation for a college project. It uses Java OOP, JDBC, MySQL, BCrypt, SLF4J/Logback and JUnit 5. The project is educational and is not designed for real banking or production use.

## Features

- Create Savings and Current accounts, with generated account numbers and BCrypt-protected PINs
- Deposit, withdraw, transfer, and check balances
- Configurable daily withdrawal limit, including cumulative withdrawals during the banking day
- View and update customer details; change a PIN; close a zero-balance account
- View date-filtered passbooks and export them to CSV or PDF
- View a savings interest estimate using a configurable rate
- Administrator sign-in, account search and zero-balance account closure
- MySQL persistence with transactional balance and ledger updates
- SwingWorker-based database calls to keep the desktop interface responsive

## Requirements

- JDK 17 or later (the project was verified with Java 21)
- Maven 3.9 or later
- MySQL 8.0

## Setup and run on Windows

Run the following commands in **PowerShell**. Keep this PowerShell window open while using the application; the environment variables are scoped to that window.

### 1. Open the project folder and check Java/Maven

```powershell
Set-Location 'C:\Users\Satish\Desktop\Banking_System'
java -version
mvn -version
```

If `java` or `mvn` is not recognized, add the installed tools to this PowerShell window's PATH. Adjust these paths if you installed them elsewhere:

```powershell
$env:Path = 'C:\Program Files\Java\jdk-21.0.12.1\bin;C:\dependencys\apache-maven-3.10.0-bin\apache-maven-3.10.0\bin;' + $env:Path
java -version
mvn -version
```

### 2. Start MySQL and initialize the schema

Check that the MySQL Windows service is running:

```powershell
Get-Service MySQL80
```

If it is stopped, start it from an elevated PowerShell window:

```powershell
Start-Service MySQL80
```

Initialize the database and tables from the project folder. MySQL will prompt for the root password:

```powershell
$mysql = 'C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe'
$schema = (Resolve-Path '.\database\schema.sql').Path.Replace('\', '/')
& $mysql -u root -p -e "source $schema"
```

If `mysql` is already on PATH, you can use `mysql` in place of the full `$mysql` executable path. The schema script is safe to run again; it creates the database and tables if they do not already exist.

### 3. Configure the application database connection

The application reads configuration from environment variables. It does **not** load `.env` automatically. Enter the MySQL password using a secure PowerShell prompt:

```powershell
$env:DB_URL = 'jdbc:mysql://localhost:3306/banking_db?serverTimezone=Asia/Kolkata'
$env:DB_USER = 'root'
$securePassword = Read-Host 'MySQL password' -AsSecureString
$env:DB_PASSWORD = [System.Net.NetworkCredential]::new('', $securePassword).Password
$env:BANKING_DAILY_LIMIT = '20000.00'
$env:SAVINGS_ANNUAL_RATE = '0.04'
```

For local development, these values connect to MySQL as `root`. For a shared or deployed environment, use a dedicated MySQL user with access only to `banking_db`. The placeholder values in `.env.example` are examples; the application does not read that file.

### 4. Launch the Swing application

```powershell
mvn exec:java
```

Maven compiles the project and launches the desktop window. Keep the terminal open while the application is running. Do not launch the generated JAR with `java -jar`; it is not packaged with its dependencies.

On the first screen, choose **Open account**, create a Savings or Current account, and save the account number displayed after creation. Then sign in with that account number and the PIN you selected. There are no preloaded customer accounts or default customer logins.

### 5. Optional: create an administrator login

Run this from the same configured PowerShell window:

```powershell
mvn exec:java "-Dexec.mainClass=app.AdminSetup" "-Dexec.args=admin"
```

Choose a username and enter an administrator password when prompted in the terminal. The password prompt does not echo the password. On the app's sign-in page, select **Administrator** to log in and search accounts. An administrator can close accounts with zero balance.

### 6. Run tests

Run the unit tests:

```powershell
mvn test
```

The MySQL integration test is opt-in. Run it only against a disposable local/test database because it creates and removes test rows:

```powershell
$env:BANKING_INTEGRATION_TESTS = 'true'
mvn test
```

## Configuration reference

| Variable | Purpose | Default |
| --- | --- | --- |
| `DB_URL` | JDBC URL for MySQL | `jdbc:mysql://localhost:3306/banking_db?serverTimezone=Asia/Kolkata` |
| `DB_USER` | MySQL username | `root` |
| `DB_PASSWORD` | MySQL password | Empty; set this before launching |
| `BANKING_DAILY_LIMIT` | Default daily withdrawal limit for newly created accounts | `20000.00` |
| `SAVINGS_ANNUAL_RATE` | Rate used for the savings interest estimate | `0.04` (4%) |

## Project structure

```text
src/main/java/app/          Swing entry point and administrator setup
src/main/java/model/        Customer, account, and transaction types
src/main/java/service/      Banking, administrator, and passbook operations
src/main/java/database/     JDBC connection configuration
src/main/java/interfaces/   Authentication, printing, and interest contracts
src/main/java/exception/    Banking exception type
src/test/java/              JUnit unit and opt-in MySQL integration tests
database/schema.sql         MySQL database and table definitions
database/sample_data.sql    Optional sample data script
docs/                       User guide, testing notes, and OOP concepts
```

## Troubleshooting

- **`mvn` or `java` is not recognized:** add the JDK and Maven `bin` folders to PATH, then open a new PowerShell window or use the PATH commands above.
- **Connection refused / communications link failure:** confirm the `MySQL80` service is running and `DB_URL` uses the right host and port.
- **Access denied for MySQL user:** check `DB_USER` and enter the correct password in `DB_PASSWORD` in the same PowerShell window.
- **Unknown database or missing tables:** rerun the schema initialization command in step 2.
- **App cannot connect after opening a new terminal:** reconfigure the environment variables; PowerShell does not preserve them between windows.
- **Administrator tool says it needs a terminal:** run the Maven command directly in PowerShell, not from an IDE output panel that does not provide a console.

## Notes and limitations

- Opening deposits are optional. When a nonzero opening deposit is supplied, the minimum is ₹500 for Savings and ₹1,000 for Current accounts.
- The savings feature displays an estimated simple annual interest amount, rounded to two decimal places; it does not credit interest to the balance.
- Banking dates use the configured system clock/time zone. The default database URL specifies `Asia/Kolkata`.
- This is an educational simulation. Do not use real customer or financial data.
