package personalfinance.infra;

import personalfinance.model.User;

import java.io.IOException;

public interface WalletStorage {
    boolean exists(String login);

    void save(User user) throws IOException;

    User load(String login) throws IOException;
}
