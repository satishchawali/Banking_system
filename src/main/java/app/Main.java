package app;

import model.*;
import service.BankingService;
import service.AdminService;
import service.AdminAccount;
import service.PassbookService;
import service.PassbookStatement;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

/**
 * Swing client. Database work runs in SwingWorker so the interface stays
 * responsive.
 */
public final class Main {
    private static final Color INK = new Color(25, 40, 58);
    private static final Color MUTED = new Color(112, 127, 143);
    private static final Color NAVY = new Color(15, 37, 61);
    private static final Color TEAL = new Color(24, 151, 137);
    private static final Color CANVAS = new Color(243, 247, 250);
    private static final Color SURFACE = Color.WHITE;
    private static final Color OUTLINE = new Color(224, 232, 238);
    private static final Font UI_FONT = new Font("Segoe UI", Font.PLAIN, 14);
    private static final Font UI_FONT_BOLD = new Font("Segoe UI", Font.BOLD, 14);
    private final BankingService bank = new BankingService();
    private final PassbookService reports = new PassbookService(bank);
    private final AdminService admins = new AdminService();
    private JFrame frame;
    private String account, pin;
    private AdminService.Session adminSession;
    private boolean busy;

    private record Receipt(BankTransaction transaction, BigDecimal remainingAllowance) {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Main().start());
    }

    private void start() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (UnsupportedLookAndFeelException | ClassNotFoundException | InstantiationException
                | IllegalAccessException ignored) {
            // Keep Swing's default look and feel when the platform one is unavailable.
        }
        applyTheme();
        frame = new JFrame("Campus Bank | Banking Management");
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent e) {
                if (JOptionPane.showConfirmDialog(frame, "Exit the application?", "Confirm",
                        JOptionPane.YES_NO_OPTION) == 0)
                    System.exit(0);
            }
        });
        frame.setSize(1160, 760);
        frame.setMinimumSize(new Dimension(980, 660));
        frame.setLocationRelativeTo(null);
        login();
        frame.setVisible(true);
    }

    private void applyTheme() {
        UIManager.put("Panel.background", CANVAS);
        UIManager.put("OptionPane.background", SURFACE);
        UIManager.put("OptionPane.messageForeground", INK);
        UIManager.put("OptionPane.messageFont", UI_FONT);
        UIManager.put("Button.font", UI_FONT_BOLD);
        UIManager.put("Label.font", UI_FONT);
        UIManager.put("TextField.font", UI_FONT);
        UIManager.put("PasswordField.font", UI_FONT);
        UIManager.put("ComboBox.font", UI_FONT);
        UIManager.put("Table.font", UI_FONT);
        UIManager.put("TableHeader.font", UI_FONT_BOLD);
        UIManager.put("ScrollPane.border", BorderFactory.createEmptyBorder());
    }

    private JPanel page(String title) {
        JPanel p = new JPanel(new BorderLayout(14, 14));
        p.setBackground(CANVAS);
        p.setBorder(BorderFactory.createEmptyBorder(28, 34, 30, 34));
        JLabel h = new JLabel(title);
        h.setFont(new Font("Segoe UI", Font.BOLD, 25));
        h.setForeground(INK);
        p.add(h, BorderLayout.NORTH);
        return p;
    }

    private JLabel fieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 12));
        label.setForeground(MUTED);
        return label;
    }

    private void styleField(JTextField field) {
        field.setFont(UI_FONT);
        field.setForeground(INK);
        field.setCaretColor(TEAL);
        field.setBackground(SURFACE);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(OUTLINE, 1, true),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)));
        field.setPreferredSize(new Dimension(240, 42));
    }

    private void styleButton(JButton button, boolean primary) {
        button.setFont(UI_FONT_BOLD);
        button.setForeground(primary ? Color.WHITE : INK);
        button.setBackground(primary ? TEAL : SURFACE);
        button.setOpaque(true);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(primary ? TEAL : OUTLINE, 1, true),
                BorderFactory.createEmptyBorder(10, 18, 10, 18)));
    }

    private void styleTable(JTable table) {
        table.setFont(UI_FONT);
        table.setRowHeight(30);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setGridColor(OUTLINE);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setFillsViewportHeight(true);
        table.setSelectionBackground(new Color(220, 242, 239));
        table.setSelectionForeground(INK);
        table.getTableHeader().setFont(UI_FONT_BOLD);
        table.getTableHeader().setBackground(NAVY);
        table.getTableHeader().setForeground(Color.WHITE);
        table.getTableHeader().setPreferredSize(new Dimension(0, 38));
        table.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable source, Object value, boolean selected,
                    boolean focused, int row, int column) {
                Component component = super.getTableCellRendererComponent(source, value, selected, focused, row, column);
                if (!selected)
                    component.setBackground(row % 2 == 0 ? SURFACE : new Color(247, 250, 252));
                setBorder(BorderFactory.createEmptyBorder(0, 9, 0, 9));
                return component;
            }
        });
    }

    private JPanel roundedPanel(int radius, Color color) {
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        panel.setOpaque(false);
        return panel;
    }

    private void showScreen(JPanel panel) {
        frame.setContentPane(panel);
        frame.revalidate();
        frame.repaint();
    }

    private <T> void runAsync(Callable<T> operation, Consumer<T> success) {
        if (busy)
            return;
        busy = true;
        frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        new SwingWorker<T, Void>() {
            protected T doInBackground() throws Exception {
                return operation.call();
            }

            protected void done() {
                busy = false;
                frame.setCursor(Cursor.getDefaultCursor());
                try {
                    success.accept(get());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    error(e);
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause();
                    error(cause instanceof Exception ? (Exception) cause : new RuntimeException(cause));
                } catch (RuntimeException e) {
                    error(e);
                }
            }
        }.execute();
    }

    private void login() {
        JPanel p = new JPanel(new GridLayout(1, 2));
        p.setBackground(CANVAS);
        JPanel hero = new JPanel(new BorderLayout(0, 26)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setPaint(new GradientPaint(0, 0, NAVY, getWidth(), getHeight(), new Color(24, 91, 105)));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        hero.setOpaque(false);
        hero.setBorder(BorderFactory.createEmptyBorder(42, 48, 42, 44));
        JLabel brand = new JLabel("◈  CAMPUS BANK");
        brand.setFont(new Font("Segoe UI", Font.BOLD, 17));
        brand.setForeground(Color.WHITE);
        hero.add(brand, BorderLayout.NORTH);
        JPanel message = new JPanel();
        message.setOpaque(false);
        message.setLayout(new BoxLayout(message, BoxLayout.Y_AXIS));
        JLabel eyebrow = new JLabel("PERSONAL BANKING");
        eyebrow.setFont(new Font("Segoe UI", Font.BOLD, 11));
        eyebrow.setForeground(new Color(143, 224, 211));
        JLabel headline = new JLabel("Banking, made simple.");
        headline.setFont(new Font("Segoe UI", Font.BOLD, 36));
        headline.setForeground(Color.WHITE);
        JLabel description = new JLabel("<html>Move money, review your passbook, and<br>manage your account from one secure place.</html>");
        description.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        description.setForeground(new Color(216, 230, 237));
        message.add(eyebrow);
        message.add(Box.createVerticalStrut(16));
        message.add(headline);
        message.add(Box.createVerticalStrut(18));
        message.add(description);
        message.add(Box.createVerticalStrut(32));
        JLabel badge = new JLabel("   ●  PIN protected     ·     MySQL backed   ");
        badge.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        badge.setForeground(Color.WHITE);
        badge.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(125, 177, 187), 1, true),
                BorderFactory.createEmptyBorder(9, 8, 9, 8)));
        message.add(badge);
        hero.add(message, BorderLayout.CENTER);
        JLabel footer = new JLabel("CAMPUS BANK  ·  SECURE CUSTOMER PORTAL");
        footer.setFont(new Font("Segoe UI", Font.BOLD, 10));
        footer.setForeground(new Color(177, 204, 214));
        hero.add(footer, BorderLayout.SOUTH);
        p.add(hero);

        JPanel right = new JPanel(new GridBagLayout());
        right.setBackground(CANVAS);
        JPanel card = roundedPanel(22, SURFACE);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(34, 36, 34, 36));
        card.setPreferredSize(new Dimension(420, 470));
        JLabel title = new JLabel("Welcome back");
        title.setFont(new Font("Segoe UI", Font.BOLD, 26));
        title.setForeground(INK);
        JLabel subtitle = new JLabel("Sign in to continue to your account.");
        subtitle.setFont(UI_FONT);
        subtitle.setForeground(MUTED);
        JTextField a = new JTextField();
        JPasswordField s = new JPasswordField();
        styleField(a);
        styleField(s);
        a.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        s.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        JPanel fields = new JPanel(new GridBagLayout());
        fields.setOpaque(false);
        fields.setAlignmentX(Component.LEFT_ALIGNMENT);
        fields.setMaximumSize(new Dimension(Integer.MAX_VALUE, 140));
        GridBagConstraints fieldConstraints = new GridBagConstraints();
        fieldConstraints.gridx = 0;
        fieldConstraints.weightx = 1;
        fieldConstraints.fill = GridBagConstraints.HORIZONTAL;
        fieldConstraints.anchor = GridBagConstraints.LINE_START;
        fieldConstraints.insets = new Insets(0, 0, 7, 0);
        fieldConstraints.gridy = 0;
        fields.add(fieldLabel("ACCOUNT NUMBER"), fieldConstraints);
        fieldConstraints.gridy = 1;
        fieldConstraints.insets = new Insets(0, 0, 15, 0);
        fields.add(a, fieldConstraints);
        fieldConstraints.gridy = 2;
        fieldConstraints.insets = new Insets(0, 0, 7, 0);
        fields.add(fieldLabel("PIN"), fieldConstraints);
        fieldConstraints.gridy = 3;
        fieldConstraints.insets = new Insets(0, 0, 0, 0);
        fields.add(s, fieldConstraints);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(title);
        card.add(Box.createVerticalStrut(7));
        card.add(subtitle);
        card.add(Box.createVerticalStrut(28));
        card.add(fields);
        card.add(Box.createVerticalStrut(22));
        JButton signIn = new JButton("Sign in");
        styleButton(signIn, true);
        signIn.setUI(new javax.swing.plaf.basic.BasicButtonUI());
        signIn.setForeground(Color.WHITE);
        signIn.setBackground(TEAL);
        signIn.setOpaque(true);
        signIn.setContentAreaFilled(true);
        signIn.setBorderPainted(true);
        signIn.setAlignmentX(Component.LEFT_ALIGNMENT);
        signIn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        signIn.setPreferredSize(new Dimension(348, 44));
        signIn.addActionListener(e -> {
            account = a.getText().trim();
            pin = new String(s.getPassword());
            runAsync(() -> bank.balance(account, pin), v -> dashboard());
        });
        card.add(signIn);
        card.add(Box.createVerticalStrut(22));
        JSeparator separator = new JSeparator();
        separator.setAlignmentX(Component.LEFT_ALIGNMENT);
        separator.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        card.add(separator);
        card.add(Box.createVerticalStrut(15));
        JLabel newCustomer = new JLabel("New to Campus Bank?");
        newCustomer.setForeground(MUTED);
        newCustomer.setFont(UI_FONT);
        newCustomer.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(newCustomer);
        card.add(Box.createVerticalStrut(10));
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT, 9, 0));
        bottom.setOpaque(false);
        bottom.setAlignmentX(Component.LEFT_ALIGNMENT);
        bottom.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        JButton create = new JButton("Open account");
        styleButton(create, false);
        create.addActionListener(e -> create());
        JButton admin = new JButton("Administrator");
        styleButton(admin, false);
        admin.addActionListener(e -> adminLogin());
        bottom.add(create);
        bottom.add(admin);
        card.add(bottom);
        right.add(card);
        p.add(right);
        showScreen(p);
    }

    private void dashboard() {
        JPanel shell = new JPanel(new BorderLayout());
        shell.setBackground(CANVAS);
        JPanel sidebar = new JPanel(new BorderLayout(0, 24));
        sidebar.setBackground(NAVY);
        sidebar.setBorder(BorderFactory.createEmptyBorder(28, 20, 24, 20));
        JPanel identity = new JPanel();
        identity.setOpaque(false);
        identity.setLayout(new BoxLayout(identity, BoxLayout.Y_AXIS));
        JLabel brand = new JLabel("◈  CAMPUS BANK");
        brand.setFont(new Font("Segoe UI", Font.BOLD, 16));
        brand.setForeground(Color.WHITE);
        JLabel portal = new JLabel("CUSTOMER PORTAL");
        portal.setFont(new Font("Segoe UI", Font.BOLD, 10));
        portal.setForeground(new Color(144, 180, 193));
        identity.add(brand);
        identity.add(Box.createVerticalStrut(7));
        identity.add(portal);
        sidebar.add(identity, BorderLayout.NORTH);

        JPanel accountCard = roundedPanel(16, new Color(29, 57, 81));
        accountCard.setLayout(new BoxLayout(accountCard, BoxLayout.Y_AXIS));
        accountCard.setBorder(BorderFactory.createEmptyBorder(16, 15, 16, 15));
        JLabel signedIn = new JLabel("SIGNED IN AS");
        signedIn.setFont(new Font("Segoe UI", Font.BOLD, 10));
        signedIn.setForeground(new Color(151, 183, 195));
        JLabel accountNumber = new JLabel("…" + account.substring(Math.max(0, account.length() - 4)));
        accountNumber.setFont(new Font("Segoe UI", Font.BOLD, 17));
        accountNumber.setForeground(Color.WHITE);
        accountCard.add(signedIn);
        accountCard.add(Box.createVerticalStrut(8));
        accountCard.add(accountNumber);
        sidebar.add(accountCard, BorderLayout.CENTER);

        JPanel sideBottom = new JPanel(new BorderLayout(0, 9));
        sideBottom.setOpaque(false);
        JLabel security = new JLabel("●  Secure session");
        security.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        security.setForeground(new Color(166, 202, 202));
        JButton logout = new JButton("Log out");
        styleButton(logout, false);
        logout.setHorizontalAlignment(SwingConstants.LEFT);
        logout.addActionListener(e -> performDashboardAction("Logout"));
        sideBottom.add(security, BorderLayout.NORTH);
        sideBottom.add(logout, BorderLayout.SOUTH);
        sidebar.add(sideBottom, BorderLayout.SOUTH);
        sidebar.setPreferredSize(new Dimension(226, 0));
        shell.add(sidebar, BorderLayout.WEST);

        JPanel content = new JPanel(new BorderLayout(0, 22));
        content.setBackground(CANVAS);
        content.setBorder(BorderFactory.createEmptyBorder(32, 36, 30, 36));
        JPanel heading = new JPanel(new BorderLayout(0, 7));
        heading.setOpaque(false);
        JLabel welcome = new JLabel("Your banking dashboard");
        welcome.setFont(new Font("Segoe UI", Font.BOLD, 27));
        welcome.setForeground(INK);
        JLabel subheading = new JLabel("Choose an action to manage your account.");
        subheading.setFont(UI_FONT);
        subheading.setForeground(MUTED);
        heading.add(welcome, BorderLayout.NORTH);
        heading.add(subheading, BorderLayout.CENTER);
        content.add(heading, BorderLayout.NORTH);

        JPanel actions = new JPanel(new BorderLayout(0, 12));
        actions.setOpaque(false);
        JLabel sectionTitle = new JLabel("ACCOUNT SERVICES");
        sectionTitle.setFont(new Font("Segoe UI", Font.BOLD, 11));
        sectionTitle.setForeground(MUTED);
        actions.add(sectionTitle, BorderLayout.NORTH);
        String[] names = { "Create account", "Deposit", "Withdraw", "Balance", "Transfer", "Passbook", "Edit details",
                "Account info", "Change PIN", "Savings interest", "Close account", "Logout" };
        String[] descriptions = { "Open another customer account", "Add funds to your account", "Withdraw within your daily limit",
                "View your current account balance", "Send money to another account", "Browse or export transactions",
                "Change your contact information", "Review your account details", "Update your security PIN",
                "View your savings estimate", "Close an account with zero balance", "End your secure session" };
        String[] symbols = { "+", "↓", "↑", "₹", "⇄", "▤", "✎", "◎", "⌑", "%", "×", "↗" };
        JPanel grid = new JPanel(new GridLayout(0, 3, 13, 13));
        grid.setOpaque(false);
        for (int i = 0; i < names.length; i++) {
            String action = names[i];
            JButton tile = new JButton("<html><div style='width:175px'><span style='font-size:20;color:#189789'>"
                    + symbols[i] + "</span><br><b>" + action + "</b><br><span style='font-size:10;color:#708090'>"
                    + descriptions[i] + "</span></div></html>");
            styleButton(tile, false);
            tile.setHorizontalAlignment(SwingConstants.LEFT);
            tile.setVerticalAlignment(SwingConstants.TOP);
            tile.setPreferredSize(new Dimension(200, 116));
            tile.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(OUTLINE, 1, true),
                    BorderFactory.createEmptyBorder(14, 15, 12, 15)));
            tile.setToolTipText(descriptions[i]);
            tile.addActionListener(e -> performDashboardAction(action));
            grid.add(tile);
        }
        actions.add(grid, BorderLayout.CENTER);
        content.add(actions, BorderLayout.CENTER);
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        JLabel note = new JLabel("Campus Bank  ·  Secure desktop banking");
        note.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        note.setForeground(MUTED);
        footer.add(note, BorderLayout.WEST);
        content.add(footer, BorderLayout.SOUTH);
        shell.add(content, BorderLayout.CENTER);
        showScreen(shell);
    }

    private void performDashboardAction(String action) {
        switch (action) {
            case "Create account" -> create();
            case "Deposit" -> transaction(false);
            case "Withdraw" -> transaction(true);
            case "Balance" -> balance();
            case "Transfer" -> transfer();
            case "Passbook" -> passbook();
            case "Edit details" -> editDetails();
            case "Account info" -> details();
            case "Change PIN" -> changePin();
            case "Savings interest" -> interest();
            case "Close account" -> closeAccount();
            case "Logout" -> {
                account = null;
                pin = null;
                login();
            }
        }
    }

    private void create() {
        JTextField name = new JTextField(), address = new JTextField(), mobile = new JTextField(),
                amount = new JTextField("0.00");
        JPasswordField secret = new JPasswordField();
        JComboBox<AccountType> type = new JComboBox<>(AccountType.values());
        for (JTextField field : List.of(name, address, mobile, amount, secret))
            styleField(field);
        type.setFont(UI_FONT);
        Object[] f = { "Full name", name, "Address", address, "Mobile", mobile, "Type", type, "Initial deposit (₹)",
                amount, "PIN (4-6 digits)", secret };
        if (JOptionPane.showConfirmDialog(frame, f, "Create account", JOptionPane.OK_CANCEL_OPTION) == 0)
            try {
                BigDecimal opening = new BigDecimal(amount.getText());
                String n = name.getText(), a = address.getText(), m = mobile.getText(), pw = new String(secret.getPassword());
                AccountType t = (AccountType) type.getSelectedItem();
                runAsync(() -> bank.createAccount(n, a, m, t, opening, pw),
                        no -> JOptionPane.showMessageDialog(frame, "Created account: " + no));
            } catch (Exception e) {
                error(e);
            }
    }

    private void transaction(boolean withdrawal) {
        String raw = JOptionPane.showInputDialog(frame, "Amount (₹)");
        if (raw == null)
            return;
        try {
            BigDecimal amount = new BigDecimal(raw);
            runAsync(() -> {
                BankTransaction t = withdrawal ? bank.withdraw(account, amount, pin)
                        : bank.deposit(account, amount, pin);
                BigDecimal left = withdrawal ? bank.withdrawalAllowance(account, pin) : null;
                return new Receipt(t, left);
            }, r -> {
                BankTransaction t = r.transaction();
                String receipt = "Receipt\nID: " + t.id() + "\nType: " + t.type() + "\nAmount: ₹" + t.amount()
                        + "\nBalance: ₹" + t.balanceAfter() + "\nDate: " + t.timestamp();
                if (withdrawal)
                    receipt += "\nRemaining daily allowance: ₹" + r.remainingAllowance();
                JOptionPane.showMessageDialog(frame, receipt);
            });
        } catch (Exception e) {
            error(e);
        }
    }

    private void balance() {
        runAsync(() -> bank.accountInfo(account, pin),
                info -> JOptionPane.showMessageDialog(frame, "Account: " + info.accountNumber() +
                        "\nType: " + info.type() + "\nStatus: " + info.status() +
                        "\nBalance: ₹" + info.balance()));
    }

    private void transfer() {
        JTextField target = new JTextField(), amount = new JTextField();
        styleField(target);
        styleField(amount);
        Object[] f = { "Recipient account", target, "Amount (₹)", amount };
        if (JOptionPane.showConfirmDialog(frame, f, "Transfer", JOptionPane.OK_CANCEL_OPTION) != 0)
            return;
        try {
            String to = target.getText().trim();
            BigDecimal value = new BigDecimal(amount.getText());
            runAsync(() -> bank.transfer(account, to, value, pin),
                    ref -> JOptionPane.showMessageDialog(frame, "Transfer reference: " + ref));
        } catch (Exception e) {
            error(e);
        }
    }

    private void passbook() {
        JTextField from = new JTextField(LocalDate.now().minusMonths(1).toString()),
                to = new JTextField(LocalDate.now().toString());
        styleField(from);
        styleField(to);
        Object[] f = { "From (YYYY-MM-DD)", from, "To (YYYY-MM-DD)", to };
        if (JOptionPane.showConfirmDialog(frame, f, "Passbook", JOptionPane.OK_CANCEL_OPTION) != 0)
            return;
        try {
            LocalDate start = LocalDate.parse(from.getText()), end = LocalDate.parse(to.getText());
            runAsync(() -> reports.statement(account, pin, start, end), this::showPassbook);
        } catch (Exception e) {
            error(e);
        }
    }

    private void showPassbook(PassbookStatement statement) {
        List<BankTransaction> tx = statement.transactions();
        String[] columns = { "Transaction ID", "Date/time", "Type", "Deposit", "Withdrawal", "Balance after" };
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        for (BankTransaction t : tx) {
            boolean credit = t.type() == TransactionType.OPENING || t.type() == TransactionType.DEPOSIT
                    || t.type() == TransactionType.TRANSFER_IN || t.type() == TransactionType.INTEREST;
            model.addRow(new Object[] { t.id(), t.timestamp(), t.type(), credit ? t.amount() : "",
                    credit ? "" : t.amount(), t.balanceAfter() });
        }
        JTable table = new JTable(model);
        table.setAutoCreateRowSorter(true);
        styleTable(table);
        JPanel view = new JPanel(new BorderLayout(8, 8));
        view.setBackground(CANVAS);
        view.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        JLabel summary = new JLabel("  Opening  ₹" + statement.openingBalance() + "       Closing  ₹"
                + statement.closingBalance() + "  ");
        summary.setFont(UI_FONT_BOLD);
        summary.setForeground(INK);
        summary.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(OUTLINE, 1, true),
                BorderFactory.createEmptyBorder(12, 6, 12, 6)));
        summary.setOpaque(true);
        summary.setBackground(SURFACE);
        view.add(summary, BorderLayout.NORTH);
        view.add(new JScrollPane(table), BorderLayout.CENTER);
        int choice = JOptionPane.showOptionDialog(frame, view, "Passbook", JOptionPane.DEFAULT_OPTION,
                JOptionPane.PLAIN_MESSAGE, null, new String[] { "Export CSV", "Export PDF", "Close" }, "Close");
        if (choice == 0) {
            JFileChooser c = new JFileChooser();
            c.setSelectedFile(new File("passbook-" + statement.accountNumber() + ".csv"));
            if (c.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION)
                runAsync(() -> {
                    try (PrintWriter w = new PrintWriter(c.getSelectedFile(), StandardCharsets.UTF_8)) {
                        w.println("Account," + statement.accountNumber());
                        w.println("From," + statement.from());
                        w.println("To," + statement.to());
                        w.println("Opening balance," + statement.openingBalance());
                        w.println("Closing balance," + statement.closingBalance());
                        w.println("transaction_id,account_number,type,amount,timestamp,balance_after,reference");
                        for (BankTransaction t : tx)
                            w.printf("%s,%s,%s,%s,%s,%s,%s%n", t.id(), t.accountNumber(), t.type(), t.amount(),
                                    t.timestamp(), t.balanceAfter(), t.reference() == null ? "" : t.reference());
                    }
                    return null;
                }, v -> JOptionPane.showMessageDialog(frame, "CSV saved."));
        } else if (choice == 1) {
            JFileChooser c = new JFileChooser();
            c.setSelectedFile(new File("passbook-" + statement.accountNumber() + ".pdf"));
            if (c.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION)
                runAsync(() -> {
                    reports.exportPdf(statement, c.getSelectedFile().toPath());
                    return null;
                }, v -> JOptionPane.showMessageDialog(frame, "PDF saved."));
        }
    }

    private void details() {
        runAsync(() -> bank.accountInfo(account, pin),
                i -> JOptionPane.showMessageDialog(frame,
                        "Name: " + i.customerName() + "\nAccount: " + i.accountNumber() + "\nAddress: " + i.address()
                                + "\nMobile: " + i.mobile() + "\nType: " + i.type() + "\nOpened: " + i.openingDate()
                                + "\nStatus: " + i.status() + "\nBalance: ₹" + i.balance()));
    }

    private void editDetails() {
        runAsync(() -> bank.accountInfo(account, pin), i -> {
            JTextField name = new JTextField(i.customerName()), address = new JTextField(i.address()),
                    mobile = new JTextField(i.mobile());
            styleField(name);
            styleField(address);
            styleField(mobile);
            Object[] f = { "Full name", name, "Address", address, "Mobile", mobile };
            if (JOptionPane.showConfirmDialog(frame, f, "Update details", JOptionPane.OK_CANCEL_OPTION) == 0) {
                String n = name.getText(), a = address.getText(), m = mobile.getText();
                runAsync(() -> {
                    bank.updateCustomer(account, pin, n, a, m);
                    return null;
                }, v -> JOptionPane.showMessageDialog(frame, "Customer details updated."));
            }
        });
    }

    private void interest() {
        String rate = System.getenv().getOrDefault("SAVINGS_ANNUAL_RATE", "0.04");
        runAsync(() -> bank.annualInterestEstimate(account, pin),
                v -> JOptionPane.showMessageDialog(frame, "Estimated simple annual interest at "
                        + new BigDecimal(rate).multiply(new BigDecimal("100")) + "%: ₹" + v));
    }

    private void changePin() {
        JPasswordField old = new JPasswordField(), next = new JPasswordField();
        Object[] f = { "Current PIN", old, "New PIN (4-6 digits)", next };
        if (JOptionPane.showConfirmDialog(frame, f, "Change PIN", JOptionPane.OK_CANCEL_OPTION) != 0)
            return;
        String oldText = new String(old.getPassword()), newText = new String(next.getPassword());
        runAsync(() -> {
            bank.changePin(account, oldText, newText);
            return null;
        }, v -> {
            pin = newText;
            JOptionPane.showMessageDialog(frame, "PIN updated.");
        });
    }

    private void closeAccount() {
        if (JOptionPane.showConfirmDialog(frame, "Close the account? Its balance must be zero.", "Confirm closure",
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION)
            return;
        runAsync(() -> {
            bank.closeAccount(account, pin);
            return null;
        }, v -> {
            JOptionPane.showMessageDialog(frame, "Account closed.");
            account = null;
            pin = null;
            login();
        });
    }

    private void adminLogin() {
        JTextField user = new JTextField();
        JPasswordField password = new JPasswordField();
        Object[] f = { "Administrator username", user, "Password", password };
        if (JOptionPane.showConfirmDialog(frame, f, "Administrator sign in", JOptionPane.OK_CANCEL_OPTION) != 0)
            return;
        String u = user.getText().trim(), pw = new String(password.getPassword());
        runAsync(() -> admins.login(u, pw), session -> {
            adminSession = session;
            adminDashboard();
        });
    }

    private void adminDashboard() {
        JPanel p = page("Administrator · Account management");
        JPanel search = new JPanel(new BorderLayout(8, 8));
        search.setOpaque(false);
        JTextField term = new JTextField();
        styleField(term);
        JButton find = new JButton("Search account or customer");
        styleButton(find, true);
        String[] cols = { "Account", "Customer", "Mobile", "Type", "Balance", "Status", "Opened" };
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        JTable table = new JTable(model);
        table.setAutoCreateRowSorter(true);
        styleTable(table);
        find.addActionListener(e -> {
            String q = term.getText();
            runAsync(() -> admins.search(adminSession, q), rows -> {
                model.setRowCount(0);
                for (AdminAccount a : rows)
                    model.addRow(new Object[] { a.accountNumber(), a.customerName(), a.mobile(), a.type(), a.balance(),
                            a.status(), a.openingDate() });
            });
        });
        search.add(term, BorderLayout.CENTER);
        search.add(find, BorderLayout.EAST);
        p.add(search, BorderLayout.NORTH);
        p.add(new JScrollPane(table), BorderLayout.CENTER);
        JButton close = new JButton("Close selected zero-balance account");
        styleButton(close, true);
        close.addActionListener(e -> {
            int r = table.getSelectedRow();
            if (r < 0)
                return;
            String no = String.valueOf(model.getValueAt(table.convertRowIndexToModel(r), 0));
            runAsync(() -> {
                admins.closeAccount(adminSession, no);
                return null;
            }, v -> {
                find.doClick();
                JOptionPane.showMessageDialog(frame, "Account closed.");
            });
        });
        JButton logout = new JButton("Administrator logout");
        styleButton(logout, false);
        logout.addActionListener(e -> {
            adminSession = null;
            login();
        });
        JPanel actions = new JPanel();
        actions.add(close);
        actions.add(logout);
        p.add(actions, BorderLayout.SOUTH);
        showScreen(p);
    }

    private void error(Exception e) {
        JOptionPane.showMessageDialog(frame, e.getMessage() == null ? "Operation failed." : e.getMessage(),
                "Banking error", JOptionPane.ERROR_MESSAGE);
    }
}
