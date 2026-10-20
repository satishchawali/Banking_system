# User guide

1. Start MySQL, initialize `database/schema.sql`, configure the DB environment variables and run `mvn exec:java`.
2. Choose **Open account**, enter name, address, mobile, type, optional opening deposit (minimum ₹500 Savings / ₹1,000 Current when nonzero) and a 4-6 digit PIN. Save the displayed account number.
3. Sign in with account number and PIN. Deposit, withdraw, transfer and balance actions authenticate through the signed-in account. Withdrawals check both available balance and remaining daily cap.
4. Use Passbook with ISO dates (`YYYY-MM-DD`) to view saved transactions chronologically in a sortable table with opening/closing balances. Export statements to CSV or PDF. Transfer legs share a reference ID. Edit details, change PIN and close a zero-balance account from the dashboard.
5. Logout clears the current UI session. Exit prompts for confirmation.

The Savings interest action estimates simple annual interest as current balance × configured annual rate, rounded half-up to cents; it does not credit the account. The PIN is never displayed after sign-in. Keep the account number and PIN private.



## Administrator

Initialize the schema and run the one-time setup utility from a PowerShell terminal: `mvn exec:java -Dexec.mainClass=app.AdminSetup -Dexec.args="admin"`. It prompts for the password without echoing it and stores a BCrypt hash. On the sign-in page choose **Administrator**, authenticate, then search by account number or customer name. Select a row to close a zero-balance account. Admin accounts are stored separately from customers.
