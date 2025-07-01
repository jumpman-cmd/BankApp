import java.awt.*;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/**
 * Main class for the Bank Application with a Swing GUI.
 * This class handles the UI and integrates with the banking logic.
 */
public class BankApp extends JFrame {

    // --- Data Models (Inner Classes for simplicity) ---

    /**
     * Represents a single financial transaction.
     */
    private static class Transaction {
        public enum Type { DEPOSIT, WITHDRAWAL, INTEREST, LOAN_TAKEN, LOAN_REPAYMENT, FEE }
        private final Type type;
        private final double amount;
        private final LocalDateTime timestamp;
        private final String description;

        public Transaction(Type type, double amount, String description) {
            this.type = type;
            this.amount = amount;
            this.timestamp = LocalDateTime.now();
            this.description = description;
        }

        @Override
        public String toString() {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            return String.format("[%s] %s: %.2f (%s)", timestamp.format(formatter), type, amount, description);
        }
    }

    /**
     * Represents a bank account.
     */
    private static class Account {
        public enum AccountType { CHECKING, SAVINGS }

        private final String accountNumber;
        private final String pin; // Simple PIN for authentication
        private String accountHolderName;
        private double balance;
        private AccountType type;
        private List<Transaction> transactionHistory;
        private double loanAmount; // Tracks outstanding loan amount

        public Account(String accountNumber, String pin, String accountHolderName, AccountType type) {
            this.accountNumber = accountNumber;
            this.pin = pin;
            this.accountHolderName = accountHolderName;
            this.type = type;
            this.balance = 0.0;
            this.transactionHistory = new ArrayList<>();
            this.loanAmount = 0.0;
        }

        public String getAccountNumber() {
            return accountNumber;
        }

        public String getPin() {
            return pin;
        }

        public String getAccountHolderName() {
            return accountHolderName;
        }

        public double getBalance() {
            return balance;
        }

        public AccountType getType() {
            return type;
        }

        public List<Transaction> getTransactionHistory() {
            return transactionHistory;
        }

        public double getLoanAmount() {
            return loanAmount;
        }

        /**
         * Deposits money into the account.
         * @param amount The amount to deposit.
         * @param description Description of the deposit.
         * @return true if successful, false otherwise.
         */
        public boolean deposit(double amount, String description) {
            if (amount > 0) {
                balance += amount;
                transactionHistory.add(new Transaction(Transaction.Type.DEPOSIT, amount, description));
                return true;
            }
            return false;
        }

        /**
         * Withdraws money from the account. Applies a fee for checking accounts.
         * @param amount The amount to withdraw.
         * @param description Description of the withdrawal.
         * @return true if successful, false otherwise (e.g., insufficient funds).
         */
        public boolean withdraw(double amount, String description) {
            double actualAmount = amount;
            double fee = 0;

            if (amount <= 0) {
                return false;
            }

            // Apply fee for checking accounts
            if (this.type == AccountType.CHECKING) {
                fee = 0.50; // Example fee
                actualAmount += fee;
            }

            if (balance >= actualAmount) {
                balance -= actualAmount;
                transactionHistory.add(new Transaction(Transaction.Type.WITHDRAWAL, amount, description));
                if (fee > 0) {
                    transactionHistory.add(new Transaction(Transaction.Type.FEE, fee, "Withdrawal Fee"));
                }
                return true;
            }
            return false; // Insufficient funds
        }

        /**
         * Applies interest to savings accounts.
         * @param rate The interest rate (e.g., 0.01 for 1%).
         * @return The calculated interest amount.
         */
        public double applyInterest(double rate) {
            if (this.type == AccountType.SAVINGS && balance > 0) {
                double interest = balance * rate;
                balance += interest;
                transactionHistory.add(new Transaction(Transaction.Type.INTEREST, interest, "Monthly Interest Earned"));
                return interest;
            }
            return 0.0;
        }

        /**
         * Takes out a loan.
         * @param loanAmt The amount of the loan.
         * @return true if successful, false otherwise.
         */
        public boolean takeLoan(double loanAmt) {
            if (loanAmt > 0 && this.loanAmount == 0) { // Only one outstanding loan at a time
                this.loanAmount = loanAmt;
                this.balance += loanAmt; // Loan amount is added to balance
                transactionHistory.add(new Transaction(Transaction.Type.LOAN_TAKEN, loanAmt, "Loan Taken"));
                return true;
            }
            return false;
        }

        /**
         * Repays a portion of the loan.
         * @param repaymentAmt The amount to repay.
         * @return true if successful, false otherwise (e.g., insufficient funds, no loan).
         */
        public boolean repayLoan(double repaymentAmt) {
            if (repaymentAmt > 0 && this.loanAmount > 0) {
                if (balance >= repaymentAmt) {
                    balance -= repaymentAmt;
                    this.loanAmount -= repaymentAmt;
                    if (this.loanAmount < 0) this.loanAmount = 0; // Ensure loan doesn't go negative
                    transactionHistory.add(new Transaction(Transaction.Type.LOAN_REPAYMENT, repaymentAmt, "Loan Repayment"));
                    return true;
                }
            }
            return false;
        }
    }

    // tHE Bank Management Logic

    private final Map<String, Account> accounts;
    private Account loggedInAccount; // Currently logged-in account

    public BankApp() {
        accounts = new HashMap<>();
        loggedInAccount = null;

        // --tHis is whEre the GUi start--
        setTitle("Money Flow Bank Application"); // Changed bank name
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // Center the window

        // Use CardLayout to switch between different panels (Login/Dashboard)
        cardPanel = new JPanel(new CardLayout());
        setupLoginPanel();
        setupDashboardPanel();
        setupCreateAccountPanel();
        setupLoanPanel();

        add(cardPanel);
        showLoginPanel(); // Start with the login panel
    }

    // --- GUI Components and Panels ---
    private JPanel cardPanel;
    private JPanel loginPanel;
    private JPanel dashboardPanel;
    private JPanel createAccountPanel;
    private JPanel loanPanel;

    // Login Panel Components
    private JTextField loginAccountField;
    private JPasswordField loginPinField;
    private JLabel loginMessageLabel;

    // Dashboard Panel Components
    private JLabel welcomeLabel;
    private JLabel balanceLabel;
    private JTextArea transactionHistoryArea;
    private JTextField amountField;
    private JScrollPane transactionScrollPane;

    // Create Account Panel Components
    private JTextField newAccountNameField;
    private JTextField newAccountPinField;
    private JComboBox<String> newAccountTypeComboBox;
    private JLabel createAccountMessageLabel;

    // Loan Panel Components
    private JTextField loanAmountField;
    private JTextField repayAmountField;
    private JLabel loanStatusLabel;
    private JLabel loanMessageLabel;


    /**
     * Sets up the login panel.
     */
    private void setupLoginPanel() {
        loginPanel = new JPanel(new GridBagLayout());
        loginPanel.setBorder(new EmptyBorder(50, 50, 50, 50)); // Padding
        loginPanel.setBackground(new Color(240, 248, 255)); // AliceBlue

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Title
        JLabel titleLabel = new JLabel("Welcome to Money Flow Bank", SwingConstants.CENTER); // Changed bank name
        titleLabel.setFont(new Font("Arial", Font.BOLD, 30));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        loginPanel.add(titleLabel, gbc);

        // Account Number
        JLabel accLabel = new JLabel("Account Number:");
        accLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.EAST;
        loginPanel.add(accLabel, gbc);

        loginAccountField = new JTextField(20);
        loginAccountField.setFont(new Font("Arial", Font.PLAIN, 16));
        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.WEST;
        loginPanel.add(loginAccountField, gbc);

        // PIN
        JLabel pinLabel = new JLabel("PIN:");
        pinLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.EAST;
        loginPanel.add(pinLabel, gbc);

        loginPinField = new JPasswordField(20);
        loginPinField.setFont(new Font("Arial", Font.PLAIN, 16));
        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.WEST;
        loginPanel.add(loginPinField, gbc);

        // Login Button
        JButton loginButton = createStyledButton("Login", new Color(70, 130, 180)); // SteelBlue
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        loginPanel.add(loginButton, gbc);

        // Create Account Button
        JButton createAccountButton = createStyledButton("Create New Account", new Color(60, 179, 113)); // MediumSeaGreen
        gbc.gridy = 4;
        loginPanel.add(createAccountButton, gbc);

        loginMessageLabel = new JLabel("", SwingConstants.CENTER);
        loginMessageLabel.setFont(new Font("Arial", Font.ITALIC, 14));
        loginMessageLabel.setForeground(Color.RED);
        gbc.gridy = 5;
        loginPanel.add(loginMessageLabel, gbc);

        // Add actions
        loginButton.addActionListener(e -> attemptLogin());
        createAccountButton.addActionListener(e -> showCreateAccountPanel());

        cardPanel.add(loginPanel, "Login");
    }

    /**
     * Sets up the create account panel.
     */
    private void setupCreateAccountPanel() {
        createAccountPanel = new JPanel(new GridBagLayout());
        createAccountPanel.setBorder(new EmptyBorder(50, 50, 50, 50));
        createAccountPanel.setBackground(new Color(240, 248, 255));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel titleLabel = new JLabel("Create New Bank Account", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 28));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        createAccountPanel.add(titleLabel, gbc);

        // Account Holder Name
        JLabel nameLabel = new JLabel("Your Name:");
        nameLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.EAST;
        createAccountPanel.add(nameLabel, gbc);

        newAccountNameField = new JTextField(20);
        newAccountNameField.setFont(new Font("Arial", Font.PLAIN, 16));
        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.WEST;
        createAccountPanel.add(newAccountNameField, gbc);

        // PIN
        JLabel pinLabel = new JLabel("Choose a PIN (4-digit):");
        pinLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.EAST;
        createAccountPanel.add(pinLabel, gbc);

        newAccountPinField = new JTextField(20);
        newAccountPinField.setFont(new Font("Arial", Font.PLAIN, 16));
        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.WEST;
        createAccountPanel.add(newAccountPinField, gbc);

        // Account Type
        JLabel typeLabel = new JLabel("Account Type:");
        typeLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.anchor = GridBagConstraints.EAST;
        createAccountPanel.add(typeLabel, gbc);

        newAccountTypeComboBox = new JComboBox<>(new String[]{"CHECKING", "SAVINGS"});
        newAccountTypeComboBox.setFont(new Font("Arial", Font.PLAIN, 16));
        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.WEST;
        createAccountPanel.add(newAccountTypeComboBox, gbc);

        // Create Button
        JButton createButton = createStyledButton("Create Account", new Color(0, 128, 0)); // Green
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        createAccountPanel.add(createButton, gbc);

        // Back to Login Button
        JButton backToLoginButton = createStyledButton("Back to Login", new Color(105, 105, 105)); // DimGray
        gbc.gridy = 5;
        createAccountPanel.add(backToLoginButton, gbc);

        createAccountMessageLabel = new JLabel("", SwingConstants.CENTER);
        createAccountMessageLabel.setFont(new Font("Arial", Font.ITALIC, 14));
        createAccountMessageLabel.setForeground(Color.BLUE);
        gbc.gridy = 6;
        createAccountPanel.add(createAccountMessageLabel, gbc);

        // Add actions
        createButton.addActionListener(e -> createNewAccount());
        backToLoginButton.addActionListener(e -> showLoginPanel());

        cardPanel.add(createAccountPanel, "CreateAccount");
    }

    /**
     * Sets up the main dashboard panel after successful login.
     */
    private void setupDashboardPanel() {
        dashboardPanel = new JPanel(new BorderLayout(20, 20)); // Padding between components
        dashboardPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        dashboardPanel.setBackground(new Color(245, 245, 220)); // Beige

        // --- Top Panel (Welcome and Balance) ---
        JPanel topPanel = new JPanel(new GridLayout(2, 1));
        topPanel.setOpaque(false); // Make it transparent to show parent background
        welcomeLabel = new JLabel("Welcome, ", SwingConstants.CENTER);
        welcomeLabel.setFont(new Font("Arial", Font.BOLD, 24));
        balanceLabel = new JLabel("Balance: R0.00", SwingConstants.CENTER); // Changed currency symbol
        balanceLabel.setFont(new Font("Arial", Font.BOLD, 28));
        balanceLabel.setForeground(new Color(0, 100, 0)); // DarkGreen
        topPanel.add(welcomeLabel);
        topPanel.add(balanceLabel);
        dashboardPanel.add(topPanel, BorderLayout.NORTH);

        // --- Center Panel (Transactions and Input) ---
        JPanel centerPanel = new JPanel(new BorderLayout(15, 15));
        centerPanel.setOpaque(false);

        // Transaction History Area
        transactionHistoryArea = new JTextArea(15, 30);
        transactionHistoryArea.setEditable(false);
        transactionHistoryArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        transactionHistoryArea.setBackground(new Color(255, 255, 240)); // Ivory
        transactionHistoryArea.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        transactionScrollPane = new JScrollPane(transactionHistoryArea);
        transactionScrollPane.setBorder(BorderFactory.createTitledBorder("Transaction History"));
        centerPanel.add(transactionScrollPane, BorderLayout.CENTER);

        // Input and Action Panel
        JPanel inputActionPanel = new JPanel(new GridBagLayout());
        inputActionPanel.setOpaque(false);
        inputActionPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0)); // Top padding
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel amountLabel = new JLabel("Amount:");
        amountLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        gbc.gridx = 0; gbc.gridy = 0;
        inputActionPanel.add(amountLabel, gbc);

        amountField = new JTextField(15);
        amountField.setFont(new Font("Arial", Font.PLAIN, 16));
        gbc.gridx = 1; gbc.gridy = 0;
        inputActionPanel.add(amountField, gbc);

        JButton depositButton = createStyledButton("Deposit", new Color(34, 139, 34)); // ForestGreen
        gbc.gridx = 2; gbc.gridy = 0;
        inputActionPanel.add(depositButton, gbc);

        JButton withdrawButton = createStyledButton("Withdraw", new Color(178, 34, 34)); // Firebrick
        gbc.gridx = 3; gbc.gridy = 0;
        inputActionPanel.add(withdrawButton, gbc);

        // Loan button
        JButton loanButton = createStyledButton("Manage Loan", new Color(255, 140, 0)); // DarkOrange
        gbc.gridx = 0; gbc.gridy = 1;
        gbc.gridwidth = 2;
        inputActionPanel.add(loanButton, gbc);

        // Apply Interest button (for savings accounts)
        JButton applyInterestButton = createStyledButton("Apply Monthly Interest", new Color(100, 149, 237)); // CornflowerBlue
        gbc.gridx = 2; gbc.gridy = 1;
        gbc.gridwidth = 2;
        inputActionPanel.add(applyInterestButton, gbc);


        centerPanel.add(inputActionPanel, BorderLayout.SOUTH);
        dashboardPanel.add(centerPanel, BorderLayout.CENTER);

        // --- Bottom Panel (Logout) ---
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomPanel.setOpaque(false);
        JButton logoutButton = createStyledButton("Logout", new Color(70, 70, 70)); // Gray
        bottomPanel.add(logoutButton);
        dashboardPanel.add(bottomPanel, BorderLayout.SOUTH);

        // Add actions
        depositButton.addActionListener(e -> handleDeposit());
        withdrawButton.addActionListener(e -> handleWithdraw());
        loanButton.addActionListener(e -> showLoanPanel());
        applyInterestButton.addActionListener(e -> handleApplyInterest());
        logoutButton.addActionListener(e -> {
            loggedInAccount = null;
            showLoginPanel();
            loginMessageLabel.setText("Logged out successfully.");
        });

        cardPanel.add(dashboardPanel, "Dashboard");
    }

    /**
     * Sets up the loan management panel.
     */
    private void setupLoanPanel() {
        loanPanel = new JPanel(new GridBagLayout());
        loanPanel.setBorder(new EmptyBorder(50, 50, 50, 50));
        loanPanel.setBackground(new Color(240, 248, 255));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel titleLabel = new JLabel("Manage Your Loan", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 28));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        loanPanel.add(titleLabel, gbc);

        loanStatusLabel = new JLabel("Current Loan: R0.00", SwingConstants.CENTER); // Changed currency symbol
        loanStatusLabel.setFont(new Font("Arial", Font.BOLD, 20));
        loanStatusLabel.setForeground(new Color(218, 165, 32)); // Goldenrod
        gbc.gridy = 1;
        loanPanel.add(loanStatusLabel, gbc);

        // Take Loan Section
        JLabel takeLoanLabel = new JLabel("Take New Loan Amount:");
        takeLoanLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.EAST;
        loanPanel.add(takeLoanLabel, gbc);

        loanAmountField = new JTextField(15);
        loanAmountField.setFont(new Font("Arial", Font.PLAIN, 16));
        gbc.gridx = 1; gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.WEST;
        loanPanel.add(loanAmountField, gbc);

        JButton takeLoanButton = createStyledButton("Take Loan", new Color(255, 99, 71)); // Tomato
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        loanPanel.add(takeLoanButton, gbc);

        // Repay Loan Section
        JLabel repayLoanLabel = new JLabel("Repay Loan Amount:");
        repayLoanLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.EAST;
        loanPanel.add(repayLoanLabel, gbc);

        repayAmountField = new JTextField(15);
        repayAmountField.setFont(new Font("Arial", Font.PLAIN, 16));
        gbc.gridx = 1; gbc.gridy = 4;
        gbc.anchor = GridBagConstraints.WEST;
        loanPanel.add(repayAmountField, gbc);

        JButton repayLoanButton = createStyledButton("Repay Loan", new Color(60, 179, 113)); // MediumSeaGreen
        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        loanPanel.add(repayLoanButton, gbc);

        // Message Label
        loanMessageLabel = new JLabel("", SwingConstants.CENTER);
        loanMessageLabel.setFont(new Font("Arial", Font.ITALIC, 14));
        loanMessageLabel.setForeground(Color.BLUE);
        gbc.gridy = 6;
        loanPanel.add(loanMessageLabel, gbc);

        // Back to Dashboard Button
        JButton backToDashboardButton = createStyledButton("Back to Dashboard", new Color(105, 105, 105)); // DimGray
        gbc.gridy = 7;
        loanPanel.add(backToDashboardButton, gbc);

        // Add actions
        takeLoanButton.addActionListener(e -> handleTakeLoan());
        repayLoanButton.addActionListener(e -> handleRepayLoan());
        backToDashboardButton.addActionListener(e -> showDashboardPanel());

        cardPanel.add(loanPanel, "LoanPanel");
    }

    /**
     * Helper method to create a styled JButton.
     * @param text Button text.
     * @param bgColor Background color.
     * @return Styled JButton.
     */
    private JButton createStyledButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 16));
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(bgColor.darker(), 2),
                BorderFactory.createEmptyBorder(10, 20, 10, 20)
        ));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(bgColor.brighter());
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(bgColor);
            }
        });
        return button;
    }


    // --- Panel Switching Logic ---

    private void showLoginPanel() {
        ((CardLayout) cardPanel.getLayout()).show(cardPanel, "Login");
        loginAccountField.setText("");
        loginPinField.setText("");
        loginMessageLabel.setText("");
    }

    private void showDashboardPanel() {
        if (loggedInAccount != null) {
            updateDashboardUI();
            ((CardLayout) cardPanel.getLayout()).show(cardPanel, "Dashboard");
        } else {
            showLoginPanel(); // Redirect to login if no account is logged in
        }
    }

    private void showCreateAccountPanel() {
        ((CardLayout) cardPanel.getLayout()).show(cardPanel, "CreateAccount");
        newAccountNameField.setText("");
        newAccountPinField.setText("");
        newAccountTypeComboBox.setSelectedIndex(0);
        createAccountMessageLabel.setText("");
    }

    private void showLoanPanel() {
        if (loggedInAccount != null) {
            updateLoanPanelUI();
            ((CardLayout) cardPanel.getLayout()).show(cardPanel, "LoanPanel");
        } else {
            showLoginPanel();
        }
    }

    // --- Banking Logic Handlers ---

    /**
     * Attempts to log in a user with the provided account number and PIN.
     */
    private void attemptLogin() {
        String accountNumber = loginAccountField.getText().trim();
        String pin = new String(loginPinField.getPassword()).trim();

        if (accounts.containsKey(accountNumber)) {
            Account account = accounts.get(accountNumber);
            if (account.getPin().equals(pin)) {
                loggedInAccount = account;
                showDashboardPanel();
                loginMessageLabel.setText("");
            } else {
                loginMessageLabel.setText("Incorrect PIN. Please try again.");
            }
        } else {
            loginMessageLabel.setText("Account not found. Please create an account or check details.");
        }
    }

    /**
     * Creates a new bank account.
     */
    private void createNewAccount() {
        String name = newAccountNameField.getText().trim();
        String pin = newAccountPinField.getText().trim();
        String accountTypeStr = (String) newAccountTypeComboBox.getSelectedItem();
        Account.AccountType type = Account.AccountType.valueOf(accountTypeStr);

        if (name.isEmpty() || pin.isEmpty()) {
            createAccountMessageLabel.setText("Name and PIN cannot be empty.");
            createAccountMessageLabel.setForeground(Color.RED);
            return;
        }
        if (pin.length() != 4 || !pin.matches("\\d{4}")) {
            createAccountMessageLabel.setText("PIN must be a 4-digit number.");
            createAccountMessageLabel.setForeground(Color.RED);
            return;
        }

        String newAccountNumber = generateAccountNumber();
        Account newAccount = new Account(newAccountNumber, pin, name, type);
        accounts.put(newAccountNumber, newAccount);
        createAccountMessageLabel.setText("Account created! Your Account Number: " + newAccountNumber);
        createAccountMessageLabel.setForeground(new Color(0, 128, 0)); // Green
        // Optionally, log in the new user immediately
        // loggedInAccount = newAccount;
        // showDashboardPanel();
    }

    /**
     * Handles a deposit transaction.
     */
    private void handleDeposit() {
        if (loggedInAccount == null) return;

        try {
            double amount = Double.parseDouble(amountField.getText());
            if (loggedInAccount.deposit(amount, "User Deposit")) {
                updateDashboardUI();
                JOptionPane.showMessageDialog(this,
                        "Successfully deposited " + formatCurrency(amount),
                        "Deposit Success", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this,
                        "Deposit amount must be positive.",
                        "Deposit Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this,
                    "Invalid amount. Please enter a number.",
                    "Input Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            amountField.setText("");
        }
    }

    /**
     * Handles a withdrawal transaction.
     */
    private void handleWithdraw() {
        if (loggedInAccount == null) return;

        try {
            double amount = Double.parseDouble(amountField.getText());
            if (loggedInAccount.withdraw(amount, "User Withdrawal")) {
                updateDashboardUI();
                JOptionPane.showMessageDialog(this,
                        "Successfully withdrew " + formatCurrency(amount) +
                                (loggedInAccount.getType() == Account.AccountType.CHECKING ? " (Fee Applied)" : ""),
                        "Withdrawal Success", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this,
                        "Insufficient funds or invalid amount.",
                        "Withdrawal Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this,
                    "Invalid amount. Please enter a number.",
                    "Input Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            amountField.setText("");
        }
    }

    /**
     * Handles taking out a loan.
     */
    private void handleTakeLoan() {
        if (loggedInAccount == null) return;

        try {
            double amount = Double.parseDouble(loanAmountField.getText());
            if (loggedInAccount.getLoanAmount() > 0) {
                loanMessageLabel.setText("You already have an outstanding loan. Repay it first.");
                loanMessageLabel.setForeground(Color.RED);
            } else if (loggedInAccount.takeLoan(amount)) {
                updateLoanPanelUI();
                updateDashboardUI(); // Update dashboard balance
                loanMessageLabel.setText("Loan of " + formatCurrency(amount) + " successfully taken.");
                loanMessageLabel.setForeground(new Color(0, 128, 0)); // Green
            } else {
                loanMessageLabel.setText("Loan amount must be positive.");
                loanMessageLabel.setForeground(Color.RED);
            }
        } catch (NumberFormatException ex) {
            loanMessageLabel.setText("Invalid amount. Please enter a number.");
            loanMessageLabel.setForeground(Color.RED);
        } finally {
            loanAmountField.setText("");
        }
    }

    /**
     * Handles repaying a loan.
     */
    private void handleRepayLoan() {
        if (loggedInAccount == null) return;

        try {
            double amount = Double.parseDouble(repayAmountField.getText());
            if (loggedInAccount.getLoanAmount() == 0) {
                loanMessageLabel.setText("You have no outstanding loan to repay.");
                loanMessageLabel.setForeground(Color.RED);
            } else if (amount > loggedInAccount.getLoanAmount()) {
                loanMessageLabel.setText("Repayment amount exceeds outstanding loan. Repay " + formatCurrency(loggedInAccount.getLoanAmount()));
                loanMessageLabel.setForeground(Color.ORANGE);
            }
            else if (loggedInAccount.repayLoan(amount)) {
                updateLoanPanelUI();
                updateDashboardUI(); // Update dashboard balance
                loanMessageLabel.setText("Successfully repaid " + formatCurrency(amount) + " of your loan.");
                loanMessageLabel.setForeground(new Color(0, 128, 0)); // Green
            } else {
                loanMessageLabel.setText("Insufficient funds to repay loan or invalid amount.");
                loanMessageLabel.setForeground(Color.RED);
            }
        } catch (NumberFormatException ex) {
            loanMessageLabel.setText("Invalid amount. Please enter a number.");
            loanMessageLabel.setForeground(Color.RED);
        } finally {
            repayAmountField.setText("");
        }
    }

    /**
     * Handles applying interest to the logged-in account (if it's a savings account).
     */
    private void handleApplyInterest() {
        if (loggedInAccount == null) return;

        if (loggedInAccount.getType() == Account.AccountType.SAVINGS) {
            double interestEarned = loggedInAccount.applyInterest(0.005); // 0.5% monthly interest
            if (interestEarned > 0) {
                updateDashboardUI();
                JOptionPane.showMessageDialog(this,
                        "Interest of " + formatCurrency(interestEarned) + " applied to your savings account!",
                        "Interest Applied", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this,
                        "No interest earned (balance is zero or not a savings account).",
                        "Interest Info", JOptionPane.INFORMATION_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(this,
                    "Interest can only be applied to Savings accounts.",
                    "Feature Not Available", JOptionPane.WARNING_MESSAGE);
        }
    }

    // --- UI Update Methods ---

    /**
     * Updates the dashboard UI with current account information.
     */
    private void updateDashboardUI() {
        if (loggedInAccount != null) {
            welcomeLabel.setText("Welcome, " + loggedInAccount.getAccountHolderName() +
                                 " (Acc: " + loggedInAccount.getAccountNumber() +
                                 " | Type: " + loggedInAccount.getType() + ")");
            balanceLabel.setText("Balance: " + formatCurrency(loggedInAccount.getBalance()));

            // Update transaction history
            transactionHistoryArea.setText("");
            if (loggedInAccount.getTransactionHistory().isEmpty()) {
                transactionHistoryArea.append("No transactions yet.");
            } else {
                for (Transaction t : loggedInAccount.getTransactionHistory()) {
                    transactionHistoryArea.append(t.toString() + "\n");
                }
            }
            // Scroll to the bottom of the transaction history
            transactionHistoryArea.setCaretPosition(transactionHistoryArea.getDocument().getLength());
        }
    }

    /**
     * Updates the loan panel UI with current loan information.
     */
    private void updateLoanPanelUI() {
        if (loggedInAccount != null) {
            loanStatusLabel.setText("Current Loan: " + formatCurrency(loggedInAccount.getLoanAmount()));
            loanMessageLabel.setText("");
            loanAmountField.setText("");
            repayAmountField.setText("");
        }
    }

    // --- Utility Methods ---

    /**
     * Generates a unique 10-digit account number.
     * @return A new unique account number string.
     */
    private String generateAccountNumber() {
        Random rand = new Random();
        String accNum;
        do {
            accNum = String.format("%010d", rand.nextLong(10_000_000_000L)); // 10 digits
        } while (accounts.containsKey(accNum)); // Ensure uniqueness
        return accNum;
    }

    /**
     * Formats a double value as currency (e.g., "R1,234.56").
     * @param amount The double value to format.
     * @return Formatted currency string.
     */
    private String formatCurrency(double amount) {
        // Use Locale for South Africa (en-ZA) to get the correct currency symbol and formatting
        NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("en", "ZA"));
        return currencyFormatter.format(amount);
    }

    /**
     * Main method to run the application.
     * @param args Command line arguments (not used).
     */
    public static void main(String[] args) {
        
        SwingUtilities.invokeLater(() -> {
            BankApp bankApp = new BankApp();
            bankApp.setVisible(true);

            
            bankApp.accounts.put("1234567890", new Account("1234567890", "1234", "Alice Smith", Account.AccountType.SAVINGS));
            bankApp.accounts.get("1234567890").deposit(1500.00, "Initial Deposit");
            bankApp.accounts.get("1234567890").withdraw(50.00, "Groceries");

            bankApp.accounts.put("0987654321", new Account("0987654321", "4321", "Bob Johnson", Account.AccountType.CHECKING));
            bankApp.accounts.get("0987654321").deposit(2500.00, "Salary");
            bankApp.accounts.get("0987654321").withdraw(100.00, "Bills");
        });
    }
}
