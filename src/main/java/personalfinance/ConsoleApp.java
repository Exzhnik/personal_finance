package personalfinance;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

public class ConsoleApp {
    private final Scanner scanner = new Scanner(System.in);
    private final UserService userService = new UserService(new WalletStorage());
    private User currentUser;

    public void run() {
        boolean running = true;
        try {
            while (running) {
                printMenu();
                if (!scanner.hasNextLine()) {
                    userService.saveAll();
                    return;
                }
                String command = scanner.nextLine().trim().toLowerCase(Locale.ROOT);
                try {
                    switch (command) {
                        case "register" -> register();
                        case "login" -> login();
                        case "income" -> addOperation(OperationType.INCOME);
                        case "expense" -> addOperation(OperationType.EXPENSE);
                        case "budget" -> setBudget();
                        case "report" -> printReport();
                        case "summary" -> printSummary();
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
            }
        } catch (IOException exception) {
            System.out.println("Ошибка работы с файлом: " + exception.getMessage());
        }
    }

    private void printMenu() {
        System.out.println();
        if (currentUser == null) {
            System.out.println("Команды: register, login, exit");
        } else {
            System.out.println("Пользователь: " + currentUser.getLogin());
            System.out.println("Команды: income, expense, budget, report, summary, transfer, logout, exit");
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

    private void addOperation(OperationType type) {
        requireLoggedIn();
        String category = readLine("Категория: ");
        BigDecimal amount = readAmount();
        currentUser.getWallet().addOperation(type, category, amount);
        System.out.println(type == OperationType.INCOME ? "Доход добавлен." : "Расход добавлен.");
        if (type == OperationType.EXPENSE) {
            printExpenseWarnings(category.trim());
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
        Wallet wallet = currentUser.getWallet();
        if (wallet.getBudgets().containsKey(category)) {
            BigDecimal remaining = wallet.getBudgetRemaining(category);
            if (remaining.signum() < 0) {
                System.out.println("Внимание: бюджет категории " + category
                        + " превышен на " + remaining.abs());
            }
        }

        BigDecimal expenses = wallet.getTotal(OperationType.EXPENSE);
        BigDecimal income = wallet.getTotal(OperationType.INCOME);
        if (expenses.compareTo(income) > 0) {
            System.out.println("Внимание: расходы превышают доходы на " + expenses.subtract(income));
        }
    }

    private void setBudget() {
        requireLoggedIn();
        String category = readLine("Категория расходов: ");
        BigDecimal limit = readAmount();
        currentUser.getWallet().setBudget(category, limit);
        System.out.println("Бюджет установлен.");
    }

    private void printReport() {
        requireLoggedIn();
        Wallet wallet = currentUser.getWallet();
        System.out.println("Общий доход: " + wallet.getTotal(OperationType.INCOME));
        System.out.println("Общие расходы: " + wallet.getTotal(OperationType.EXPENSE));
        printCategoryTotals("Доходы по категориям:", wallet.getTotalsByCategory(OperationType.INCOME));
        printCategoryTotals("Расходы по категориям:", wallet.getTotalsByCategory(OperationType.EXPENSE));

        Map<String, BigDecimal> budgets = wallet.getBudgets();
        System.out.println("Бюджеты:");
        if (budgets.isEmpty()) {
            System.out.println("  Нет данных");
            return;
        }
        for (Map.Entry<String, BigDecimal> entry : budgets.entrySet()) {
            String category = entry.getKey();
            BigDecimal spent = wallet.getTotalByCategory(OperationType.EXPENSE, category);
            System.out.println("  " + category + ": лимит " + entry.getValue()
                    + ", потрачено " + spent
                    + ", остаток " + wallet.getBudgetRemaining(category));
        }
    }

    private void printCategoryTotals(String title, Map<String, BigDecimal> totals) {
        System.out.println(title);
        if (totals.isEmpty()) {
            System.out.println("  Нет данных");
            return;
        }
        for (Map.Entry<String, BigDecimal> entry : totals.entrySet()) {
            System.out.println("  " + entry.getKey() + ": " + entry.getValue());
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
