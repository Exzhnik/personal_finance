package personalfinance.model;

public class User {
    private final String login;
    private final String password;
    private final Wallet wallet;

    public User(String login, String password, Wallet wallet) {
        this.login = normalizeLogin(login);
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Пароль не может быть пустым");
        }
        if (wallet == null) {
            throw new IllegalArgumentException("Кошелёк пользователя не задан");
        }
        this.password = password;
        this.wallet = wallet;
    }

    public static String normalizeLogin(String login) {
        if (login == null || login.isBlank()) {
            throw new IllegalArgumentException("Логин не может быть пустым");
        }
        return login.trim();
    }

    public String getLogin() {
        return login;
    }

    public Wallet getWallet() {
        return wallet;
    }

    public String getPassword() {
        return password;
    }

    public boolean passwordMatches(String password) {
        return this.password.equals(password);
    }
}
