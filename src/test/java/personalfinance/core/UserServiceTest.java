package personalfinance.core;

import org.junit.jupiter.api.Test;
import personalfinance.infra.WalletStorage;
import personalfinance.model.OperationType;
import personalfinance.model.User;
import personalfinance.model.Wallet;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserServiceTest {
    private final InMemoryWalletStorage storage = new InMemoryWalletStorage();
    private final UserService userService = new UserService(storage);

    @Test
    void registersAndAuthenticatesUser() throws IOException {
        User registered = userService.register("alice", "secret");

        User authenticated = userService.login("alice", "secret");

        assertSame(registered, authenticated);
        assertEquals("alice", authenticated.getLogin());
    }

    @Test
    void rejectsDuplicateRegistrationFromMemory() throws IOException {
        userService.register("alice", "secret");

        assertThrows(IllegalArgumentException.class,
                () -> userService.register("alice", "other"));
    }

    @Test
    void rejectsDuplicateRegistrationFromStorage() {
        storage.users.put("alice", new User("alice", "secret", new Wallet()));

        assertThrows(IllegalArgumentException.class,
                () -> userService.register("alice", "other"));
    }

    @Test
    void rejectsInvalidPassword() throws IOException {
        userService.register("alice", "secret");

        assertThrows(IllegalArgumentException.class, () -> userService.login("alice", "wrong"));
    }

    @Test
    void transfersBetweenUsers() throws IOException {
        User sender = userService.register("alice", "secret");
        User recipient = userService.register("bob", "secret");

        userService.transfer(sender, "bob", new BigDecimal("250"));

        assertEquals(new BigDecimal("-250"), sender.getWallet().getBalance());
        assertEquals(new BigDecimal("250"), recipient.getWallet().getBalance());
        assertEquals(new BigDecimal("250"),
                sender.getWallet().getTotal(OperationType.EXPENSE));
        assertEquals(new BigDecimal("250"),
                recipient.getWallet().getTotal(OperationType.INCOME));
    }

    @Test
    void rejectsTransferToSelf() throws IOException {
        User sender = userService.register("alice", "secret");

        assertThrows(IllegalArgumentException.class,
                () -> userService.transfer(sender, "alice", BigDecimal.TEN));
    }

    @Test
    void rejectsTransferToUnknownUser() throws IOException {
        User sender = userService.register("alice", "secret");

        assertThrows(IllegalArgumentException.class,
                () -> userService.transfer(sender, "nobody", BigDecimal.TEN));
    }

    @Test
    void rejectsNonPositiveTransfer() throws IOException {
        User sender = userService.register("alice", "secret");
        userService.register("bob", "secret");

        assertThrows(IllegalArgumentException.class,
                () -> userService.transfer(sender, "bob", BigDecimal.ZERO));
        assertThrows(IllegalArgumentException.class,
                () -> userService.transfer(sender, "bob", new BigDecimal("-1")));
    }

    @Test
    void savesAllUsersAffectedByTransfer() throws IOException {
        User sender = userService.register("alice", "secret");
        User recipient = userService.register("bob", "secret");
        userService.transfer(sender, "bob", BigDecimal.TEN);

        userService.saveAll();

        assertEquals(2, storage.users.size());
        assertEquals(new BigDecimal("-10"), storage.users.get("alice").getWallet().getBalance());
        assertEquals(new BigDecimal("10"), storage.users.get("bob").getWallet().getBalance());
    }

    private static class InMemoryWalletStorage implements WalletStorage {
        private final Map<String, User> users = new HashMap<>();

        @Override
        public boolean exists(String login) {
            return users.containsKey(login);
        }

        @Override
        public void save(User user) {
            users.put(user.getLogin(), user);
        }

        @Override
        public User load(String login) {
            return users.get(login);
        }
    }
}
