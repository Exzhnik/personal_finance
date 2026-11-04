package personalfinance;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

public class ConsoleApp {
    private static final BigDecimal BUDGET_WARNING_THRESHOLD = new BigDecimal("0.8");

    private final Scanner scanner = new Scanner(System.in);
    private final UserService userService = new UserService(new WalletStorage());
    private final CsvWalletStorage csvWalletStorage = new CsvWalletStorage();
    private User currentUser;

    public void run() {
        boolean running = true;
        while (running) {
            printMenu();
            try {
                if (!scanner.hasNextLine()) {
                    userService.saveAll();
                    return;
                }
                String command = scanner.nextLine().trim().toLowerCase(Locale.ROOT);
                try {
                    switch (command) {
                        case "help" -> printHelp();
                        case "register" -> register();
                        case "login" -> login();
                        case "income" -> addOperation(OperationType.INCOME);
                        case "expense" -> addOperation(OperationType.EXPENSE);
                        case "budget" -> setBudget();
                        case "rename-category" -> renameCategory();
                        case "report" -> printReport();
                        case "summary" -> printSummary();
                        case "export" -> exportWallet();
                        case "import" -> importWallet();
                        case "transfer" -> transfer();
                        case "logout" -> logout();
                        case "exit" -> {
                            userService.saveAll();
                            System.out.println("Данные сохранены. Работа завершена.");
                            running = false;
                        }
                        case "" -> {
                        }
                        default -> System.out.println("Неизвестная команда: " + command);
                    }
                } catch (IllegalArgumentException exception) {
                    System.out.println("Ошибка: " + exception.getMessage());
                }
            } catch (IOException exception) {
                System.out.println("Ошибка работы с файлом: " + exception.getMessage());
            }
        }
    }

    private void printMenu() {
        System.out.println();
        if (currentUser == null) {
            System.out.println("Команды: help, register, login, exit");
        } else {
            System.out.println("Пользователь: " + currentUser.getLogin());
            System.out.println("Команды: help, income, expense, budget, rename-category, report, summary, export, import, transfer, logout, exit");
        }
        System.out.print("> ");
    }

    private void register() throws IOException {
        requireLoggedOut();
        String login = readLine("Логин: ");
        String password = readLine("Пароль: ");
        userService.register(login, password);
        System.out.println("Пользователь зарегистрирован. Теперь выполните вход командой login.");
    }

    private void login() throws IOException {
        requireLoggedOut();
        String login = readLine("Логин: ");
        String password = readLine("Пароль: ");
        currentUser = userService.login(login, password);
        System.out.println("Вход выполнен: " + currentUser.getLogin());
    }

    private void printHelp() {
        System.out.println("Команды:");
        System.out.printf("%-18s %s%n", "help", "Показать справку и примеры");
        System.out.printf("%-18s %s%n", "register", "Зарегистрировать пользователя");
        System.out.printf("%-18s %s%n", "login", "Войти по логину и паролю");
        System.out.printf("%-18s %s%n", "income", "Добавить доход");
        System.out.printf("%-18s %s%n", "expense", "Добавить расход");
        System.out.printf("%-18s %s%n", "budget", "Установить или изменить бюджет категории");
        System.out.printf("%-18s %s%n", "rename-category", "Переименовать категорию в операциях и бюджете");
        System.out.printf("%-18s %s%n", "report", "Показать общий отчёт");
        System.out.printf("%-18s %s%n", "summary", "Посчитать сумму по выбранным категориям");
        System.out.printf("%-18s %s%n", "export", "Экспортировать кошелёк в CSV-файл");
        System.out.printf("%-18s %s%n", "import", "Импортировать кошелёк из CSV-файла");
        System.out.printf("%-18s %s%n", "transfer", "Перевести средства другому пользователю");
        System.out.printf("%-18s %s%n", "logout", "Выйти из аккаунта");
        System.out.printf("%-18s %s%n", "exit", "Сохранить данные и завершить работу");
        System.out.println("Примеры:");
        System.out.println("  income → Зарплата → 50000");
        System.out.println("  budget → Еда → 4000");
        System.out.println("  rename-category → Еда → Продукты");
        System.out.println("  summary → expense → Еда,Такси");
        System.out.println("  export → exports/wallet.csv");
        System.out.println("  import → exports/wallet.csv");
        System.out.println("  transfer → bob → 250");
    }

    private void addOperation(OperationType type) {
        requireLoggedIn();
        String category = readLine("Категория: ");
        BigDecimal amount = readAmount();
        currentUser.getWallet().addOperation(type, category, amount);
        System.out.println(type == OperationType.INCOME ? "Доход добавлен." : "Расход добавлен.");
        if (type == OperationType.EXPENSE) {
            printExpenseWarnings(category.trim());
        } else {
            printZeroBalanceWarning();
        }
    }

    private void transfer() throws IOException {
        requireLoggedIn();
        String recipientLogin = readLine("Логин получателя: ").trim();
        BigDecimal amount = readAmount();
        userService.transfer(currentUser, recipientLogin, amount);
        System.out.println("Перевод выполнен.");
        printExpenseWarnings("Перевод пользователю " + recipientLogin);
    }

    private void printSummary() {
        requireLoggedIn();
        String typeInput = readLine("Тип операций (income/expense): ").trim().toLowerCase(Locale.ROOT);
        OperationType type;
        if (typeInput.equals("income")) {
            type = OperationType.INCOME;
        } else if (typeInput.equals("expense")) {
            type = OperationType.EXPENSE;
        } else {
            throw new IllegalArgumentException("Тип операций должен быть income или expense");
        }

        Set<String> categories = new LinkedHashSet<>();
        for (String category : readLine("Категории через запятую: ").split(",")) {
            if (!category.isBlank()) {
                categories.add(category.trim());
            }
        }
        if (categories.isEmpty()) {
            throw new IllegalArgumentException("Укажите хотя бы одну категорию");
        }

        Map<String, BigDecimal> available = currentUser.getWallet().getTotalsByCategory(type);
        for (String category : categories) {
            if (!available.containsKey(category)) {
                System.out.println("Категория не найдена: " + category);
            }
        }
        BigDecimal total = currentUser.getWallet().getTotalByCategories(type, categories);
        System.out.println("Сумма по выбранным категориям: " + total);
    }

    private void printExpenseWarnings(String category) {
        printBudgetWarning(category);
        Wallet wallet = currentUser.getWallet();
        BigDecimal expenses = wallet.getTotal(OperationType.EXPENSE);
        BigDecimal income = wallet.getTotal(OperationType.INCOME);
        if (expenses.compareTo(income) > 0) {
            System.out.println("Внимание: расходы превышают доходы на " + expenses.subtract(income));
        }
        printZeroBalanceWarning();
    }

    private void printBudgetWarning(String category) {
        Wallet wallet = currentUser.getWallet();
        if (!wallet.getBudgets().containsKey(category)) {
            return;
        }

        BigDecimal budget = wallet.getBudgets().get(category);
        BigDecimal spent = wallet.getTotalByCategory(OperationType.EXPENSE, category);
        BigDecimal remaining = wallet.getBudgetRemaining(category);
        if (remaining.signum() < 0) {
            System.out.println("Внимание: бюджет категории " + category
                    + " превышен на " + remaining.abs());
        } else if (remaining.signum() == 0) {
            System.out.println("Внимание: бюджет категории " + category + " полностью использован");
        } else if (budget.signum() > 0
                && spent.compareTo(budget.multiply(BUDGET_WARNING_THRESHOLD)) >= 0) {
            System.out.println("Внимание: использовано не менее 80% бюджета категории " + category);
        }
    }

    private void printZeroBalanceWarning() {
        if (currentUser.getWallet().getBalance().signum() == 0) {
            System.out.println("Внимание: текущий баланс равен нулю");
        }
    }

    private void setBudget() {
        requireLoggedIn();
        String category = readLine("Категория расходов: ");
        BigDecimal limit = readAmount();
        Wallet wallet = currentUser.getWallet();
        boolean updating = wallet.getBudgets().containsKey(category.trim());
        wallet.setBudget(category, limit);
        System.out.println(updating ? "Бюджет обновлён." : "Бюджет установлен.");
        printBudgetWarning(category.trim());
    }

    private void renameCategory() {
        requireLoggedIn();
        String currentName = readLine("Текущая категория: ");
        String newName = readLine("Новая категория: ");
        currentUser.getWallet().renameCategory(currentName, newName);
        System.out.println("Категория переименована.");
    }

    private void exportWallet() throws IOException {
        requireLoggedIn();
        Path file = readPath();
        csvWalletStorage.exportWallet(currentUser.getWallet(), file);
        System.out.println("Кошелёк экспортирован: " + file);
    }

    private void importWallet() throws IOException {
        requireLoggedIn();
        Path file = readPath();
        Wallet imported = csvWalletStorage.importWallet(file);
        currentUser.getWallet().replaceWith(imported);
        System.out.println("Кошелёк импортирован: " + file);
    }

    private Path readPath() {
        String value = readLine("Путь к CSV-файлу: ");
        if (value.isBlank()) {
            throw new IllegalArgumentException("Путь к файлу не может быть пустым");
        }
        return Path.of(value.trim());
    }

    private void printReport() {
        requireLoggedIn();
        Wallet wallet = currentUser.getWallet();
        System.out.printf("Общий доход: %s%n", wallet.getTotal(OperationType.INCOME));
        System.out.printf("Общие расходы: %s%n", wallet.getTotal(OperationType.EXPENSE));
        System.out.printf("Текущий баланс: %s%n", wallet.getBalance());
        printCategoryTotals("Доходы по категориям:", wallet.getTotalsByCategory(OperationType.INCOME));
        printCategoryTotals("Расходы по категориям:", wallet.getTotalsByCategory(OperationType.EXPENSE));

        Map<String, BigDecimal> budgets = wallet.getBudgets();
        System.out.println("Бюджеты:");
        if (budgets.isEmpty()) {
            System.out.println("  Нет данных");
            return;
        }
        System.out.printf("%-24s %12s %12s %12s%n", "Категория", "Лимит", "Потрачено", "Остаток");
        for (Map.Entry<String, BigDecimal> entry : budgets.entrySet()) {
            String category = entry.getKey();
            BigDecimal spent = wallet.getTotalByCategory(OperationType.EXPENSE, category);
            System.out.printf("%-24s %12s %12s %12s%n", category, entry.getValue(), spent,
                    wallet.getBudgetRemaining(category));
        }
    }

    private void printCategoryTotals(String title, Map<String, BigDecimal> totals) {
        System.out.println(title);
        if (totals.isEmpty()) {
            System.out.println("  Нет данных");
            return;
        }
        System.out.printf("%-24s %12s%n", "Категория", "Сумма");
        for (Map.Entry<String, BigDecimal> entry : totals.entrySet()) {
            System.out.printf("%-24s %12s%n", entry.getKey(), entry.getValue());
        }
    }

    private void logout() {
        requireLoggedIn();
        currentUser = null;
        System.out.println("Вы вышли из аккаунта.");
    }

    private BigDecimal readAmount() {
        String value = readLine("Сумма: ");
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Сумма должна быть числом, например 100.50");
        }
    }

    private String readLine(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine();
    }

    private void requireLoggedIn() {
        if (currentUser == null) {
            throw new IllegalArgumentException("Сначала выполните вход командой login");
        }
    }

    private void requireLoggedOut() {
        if (currentUser != null) {
            throw new IllegalArgumentException("Сначала выйдите из аккаунта командой logout");
        }
    }
}
