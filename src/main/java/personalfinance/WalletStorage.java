package personalfinance;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class WalletStorage {
    private final Path directory;

    public WalletStorage() {
        this(Path.of("data"));
    }

    public WalletStorage(Path directory) {
        this.directory = directory;
    }

    public boolean exists(String login) {
        return Files.exists(fileFor(login));
    }

    public void save(User user) throws IOException {
        Properties properties = new Properties();
        properties.setProperty("login", user.getLogin());
        properties.setProperty("password", user.getPassword());

        List<Operation> operations = user.getWallet().getOperations();
        properties.setProperty("operation.count", String.valueOf(operations.size()));
        for (int i = 0; i < operations.size(); i++) {
            Operation operation = operations.get(i);
            properties.setProperty("operation." + i + ".type", operation.type().name());
            properties.setProperty("operation." + i + ".category", operation.category());
            properties.setProperty("operation." + i + ".amount", operation.amount().toPlainString());
        }

        Map<String, BigDecimal> budgets = user.getWallet().getBudgets();
        properties.setProperty("budget.count", String.valueOf(budgets.size()));
        int index = 0;
        for (Map.Entry<String, BigDecimal> entry : budgets.entrySet()) {
            properties.setProperty("budget." + index + ".category", entry.getKey());
            properties.setProperty("budget." + index + ".limit", entry.getValue().toPlainString());
            index++;
        }

        Files.createDirectories(directory);
        try (OutputStream output = Files.newOutputStream(fileFor(user.getLogin()))) {
            properties.store(output, "Personal finance wallet");
        }
    }

    public User load(String login) throws IOException {
        Path file = fileFor(login);
        if (Files.notExists(file)) {
            return null;
        }

        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(file)) {
            properties.load(input);
        }

        try {
            Wallet wallet = new Wallet();
            int operationCount = Integer.parseInt(required(properties, "operation.count"));
            for (int i = 0; i < operationCount; i++) {
                OperationType type = OperationType.valueOf(required(properties, "operation." + i + ".type"));
                String category = required(properties, "operation." + i + ".category");
                BigDecimal amount = new BigDecimal(required(properties, "operation." + i + ".amount"));
                wallet.addOperation(type, category, amount);
            }

            int budgetCount = Integer.parseInt(required(properties, "budget.count"));
            for (int i = 0; i < budgetCount; i++) {
                String category = required(properties, "budget." + i + ".category");
                BigDecimal limit = new BigDecimal(required(properties, "budget." + i + ".limit"));
                wallet.setBudget(category, limit);
            }

            return new User(required(properties, "login"), required(properties, "password"), wallet);
        } catch (RuntimeException exception) {
            throw new IOException("Некорректные данные в файле пользователя: " + login, exception);
        }
    }

    private String required(Properties properties, String key) throws IOException {
        String value = properties.getProperty(key);
        if (value == null) {
            throw new IOException("В файле данных отсутствует поле: " + key);
        }
        return value;
    }

    private Path fileFor(String login) {
        String normalizedLogin = User.normalizeLogin(login);
        String fileName = URLEncoder.encode(normalizedLogin, StandardCharsets.UTF_8) + ".properties";
        return directory.resolve(fileName);
    }
}
