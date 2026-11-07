package personalfinance.infra;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import personalfinance.model.OperationType;
import personalfinance.model.User;
import personalfinance.model.Wallet;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PropertiesWalletStorageIntegrationTest {
    @TempDir
    Path directory;

    @Test
    void savesAndLoadsWallet() throws IOException {
        PropertiesWalletStorage storage = new PropertiesWalletStorage(directory);
        Wallet wallet = new Wallet();
        wallet.addOperation(OperationType.INCOME, "Зарплата", new BigDecimal("50000"));
        wallet.addOperation(OperationType.EXPENSE, "Еда", new BigDecimal("800.50"));
        wallet.setBudget("Еда", new BigDecimal("4000"));
        User user = new User("alice", "secret", wallet);

        storage.save(user);
        User loaded = storage.load("alice");

        assertEquals("alice", loaded.getLogin());
        assertTrue(loaded.passwordMatches("secret"));
        assertEquals(new BigDecimal("49199.50"), loaded.getWallet().getBalance());
        assertEquals(new BigDecimal("3199.50"), loaded.getWallet().getBudgetRemaining("Еда"));
        assertEquals(2, loaded.getWallet().getOperations().size());
    }

    @Test
    void returnsNullForMissingUser() throws IOException {
        PropertiesWalletStorage storage = new PropertiesWalletStorage(directory);

        assertNull(storage.load("missing"));
    }

    @Test
    void rejectsCorruptedData() throws IOException {
        Files.writeString(directory.resolve("alice.properties"), "operation.count=1\n");
        PropertiesWalletStorage storage = new PropertiesWalletStorage(directory);

        assertThrows(IOException.class, () -> storage.load("alice"));
    }

    @Test
    void rejectsNegativeOperationCount() throws IOException {
        assertInvalidFile("login=alice\npassword=secret\noperation.count=-1\nbudget.count=0\n");
    }

    @Test
    void rejectsNegativeBudgetCount() throws IOException {
        assertInvalidFile("login=alice\npassword=secret\noperation.count=0\nbudget.count=-1\n");
    }

    @Test
    void rejectsMismatchedLogin() throws IOException {
        assertInvalidFile("login=bob\npassword=secret\noperation.count=0\nbudget.count=0\n");
    }

    @Test
    void rejectsMalformedPropertiesEncoding() throws IOException {
        assertInvalidFile("login=alice\npassword=" + "\\" + "uZZZZ\n");
    }

    private void assertInvalidFile(String content) throws IOException {
        Path file = directory.resolve("alice.properties");
        Files.writeString(file, content);
        PropertiesWalletStorage storage = new PropertiesWalletStorage(directory);

        assertThrows(IOException.class, () -> storage.load("alice"));
        assertEquals(content, Files.readString(file));
    }
}
