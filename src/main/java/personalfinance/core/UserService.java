package personalfinance.core;

import personalfinance.infra.WalletStorage;
import personalfinance.model.OperationType;
import personalfinance.model.User;
import personalfinance.model.Wallet;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

public class UserService {
    private final Map<String, User> users = new LinkedHashMap<>();
    private final WalletStorage storage;

    public UserService(WalletStorage storage) {
        this.storage = storage;
    }

    public User register(String login, String password) throws IOException {
        String normalizedLogin = User.normalizeLogin(login);
        if (users.containsKey(normalizedLogin) || storage.exists(normalizedLogin)) {
            throw new IllegalArgumentException("Пользователь уже существует: " + normalizedLogin);
        }
        User user = new User(normalizedLogin, password, new Wallet());
        users.put(normalizedLogin, user);
        return user;
    }

    public User login(String login, String password) throws IOException {
        User user = findUser(login);
        if (user == null || !user.passwordMatches(password)) {
            throw new IllegalArgumentException("Неверный логин или пароль");
        }
        return user;
    }

    public void transfer(User sender, String recipientLogin, BigDecimal amount) throws IOException {
        if (sender == null) {
            throw new IllegalArgumentException("Отправитель не задан");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Сумма перевода должна быть больше нуля");
        }

        User recipient = findUser(recipientLogin);
        if (recipient == null) {
            throw new IllegalArgumentException("Получатель не найден: " + recipientLogin);
        }
        if (sender.getLogin().equals(recipient.getLogin())) {
            throw new IllegalArgumentException("Нельзя перевести средства самому себе");
        }

        sender.getWallet().addOperation(
                OperationType.EXPENSE, "Перевод пользователю " + recipient.getLogin(), amount
        );
        recipient.getWallet().addOperation(
                OperationType.INCOME, "Перевод от " + sender.getLogin(), amount
        );
    }

    private User findUser(String login) throws IOException {
        String normalizedLogin = User.normalizeLogin(login);
        User user = users.get(normalizedLogin);
        if (user == null) {
            user = storage.load(normalizedLogin);
            if (user != null) {
                users.put(normalizedLogin, user);
            }
        }
        return user;
    }

    public void saveAll() throws IOException {
        for (User user : users.values()) {
            storage.save(user);
        }
    }
}
