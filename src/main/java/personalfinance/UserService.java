package personalfinance;

import java.io.IOException;
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
        String normalizedLogin = User.normalizeLogin(login);
        User user = users.get(normalizedLogin);
        if (user == null) {
            user = storage.load(normalizedLogin);
            if (user != null) {
                users.put(normalizedLogin, user);
            }
        }
        if (user == null || !user.passwordMatches(password)) {
            throw new IllegalArgumentException("Неверный логин или пароль");
        }
        return user;
    }

    public void saveAll() throws IOException {
        for (User user : users.values()) {
            storage.save(user);
        }
    }
}
