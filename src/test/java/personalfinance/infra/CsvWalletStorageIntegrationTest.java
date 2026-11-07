package personalfinance.infra;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import personalfinance.model.OperationType;
import personalfinance.model.Wallet;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CsvWalletStorageIntegrationTest {
    @TempDir
    Path directory;

    private final CsvWalletStorage storage = new CsvWalletStorage();

    @Test
    void exportsAndImportsWalletWithEscapedValues() throws IOException {
        String category = "Еда, \"дом\"\nОбед";
        Wallet wallet = new Wallet();
        wallet.addOperation(OperationType.EXPENSE, category, new BigDecimal("100.50"));
        wallet.setBudget(category, new BigDecimal("1000"));
        Path file = directory.resolve("wallet.csv");

        storage.exportWallet(wallet, file);
        Wallet imported = storage.importWallet(file);

        assertEquals(1, imported.getOperations().size());
        assertEquals(category, imported.getOperations().getFirst().category());
        assertEquals(new BigDecimal("100.50"), imported.getTotal(OperationType.EXPENSE));
        assertEquals(new BigDecimal("899.50"), imported.getBudgetRemaining(category));
    }

    @Test
    void rejectsInvalidHeader() throws IOException {
        Path file = directory.resolve("invalid.csv");
        Files.writeString(file, "wrong\n");

        assertThrows(IOException.class, () -> storage.importWallet(file));
    }

    @Test
    void rejectsUnclosedQuote() throws IOException {
        Path file = directory.resolve("invalid.csv");
        Files.writeString(file, "recordType,operationType,category,amount\n\"OPERATION");

        assertThrows(IOException.class, () -> storage.importWallet(file));
    }

    @Test
    void rejectsInvalidRecord() throws IOException {
        Path file = directory.resolve("invalid.csv");
        Files.writeString(file, "recordType,operationType,category,amount\n"
                + "\"OPERATION\",\"INCOME\",\"Еда\",\"-100\"\n");

        assertThrows(IOException.class, () -> storage.importWallet(file));
    }
}
