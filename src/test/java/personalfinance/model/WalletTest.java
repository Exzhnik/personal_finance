package personalfinance.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WalletTest {
    @Test
    void calculatesTotalsAndBalance() {
        Wallet wallet = new Wallet();
        wallet.addOperation(OperationType.INCOME, "Зарплата", new BigDecimal("60000"));
        wallet.addOperation(OperationType.INCOME, "Бонус", new BigDecimal("3000"));
        wallet.addOperation(OperationType.EXPENSE, "Еда", new BigDecimal("800"));

        assertEquals(new BigDecimal("63000"), wallet.getTotal(OperationType.INCOME));
        assertEquals(new BigDecimal("800"), wallet.getTotal(OperationType.EXPENSE));
        assertEquals(new BigDecimal("62200"), wallet.getBalance());
    }

    @Test
    void calculatesTotalByCategory() {
        Wallet wallet = walletWithExpenses();

        assertEquals(new BigDecimal("800"),
                wallet.getTotalByCategory(OperationType.EXPENSE, "Еда"));
    }

    @Test
    void calculatesTotalForSelectedCategories() {
        Wallet wallet = walletWithExpenses();

        assertEquals(new BigDecimal("2300"),
                wallet.getTotalByCategories(OperationType.EXPENSE, Set.of("Еда", "Такси")));
    }

    @Test
    void groupsTotalsByCategory() {
        Wallet wallet = new Wallet();
        wallet.addOperation(OperationType.INCOME, "Зарплата", new BigDecimal("20000"));
        wallet.addOperation(OperationType.INCOME, "Зарплата", new BigDecimal("40000"));
        wallet.addOperation(OperationType.INCOME, "Бонус", new BigDecimal("3000"));

        Map<String, BigDecimal> totals = wallet.getTotalsByCategory(OperationType.INCOME);

        assertEquals(new BigDecimal("60000"), totals.get("Зарплата"));
        assertEquals(new BigDecimal("3000"), totals.get("Бонус"));
    }

    @Test
    void calculatesBudgetRemaining() {
        Wallet wallet = walletWithExpenses();
        wallet.setBudget("Еда", new BigDecimal("4000"));

        assertEquals(new BigDecimal("3200"), wallet.getBudgetRemaining("Еда"));
    }

    @Test
    void renamesCategoryInOperationsAndBudget() {
        Wallet wallet = walletWithExpenses();
        wallet.setBudget("Еда", new BigDecimal("4000"));

        wallet.renameCategory("Еда", "Продукты");

        assertEquals(new BigDecimal("800"),
                wallet.getTotalByCategory(OperationType.EXPENSE, "Продукты"));
        assertEquals(new BigDecimal("4000"), wallet.getBudgets().get("Продукты"));
        assertEquals(BigDecimal.ZERO, wallet.getTotalByCategory(OperationType.EXPENSE, "Еда"));
    }

    @Test
    void rejectsRenameWhenCategoryMissing() {
        Wallet wallet = new Wallet();

        assertThrows(IllegalArgumentException.class,
                () -> wallet.renameCategory("Отсутствует", "Новая"));
    }

    @Test
    void rejectsRenameWhenTargetExists() {
        Wallet wallet = walletWithExpenses();

        assertThrows(IllegalArgumentException.class,
                () -> wallet.renameCategory("Еда", "Такси"));
    }

    @Test
    void replacesWalletWithImportedData() {
        Wallet source = new Wallet();
        source.addOperation(OperationType.INCOME, "Зарплата", new BigDecimal("5000"));
        source.setBudget("Еда", new BigDecimal("1000"));
        Wallet target = walletWithExpenses();

        target.replaceWith(source);

        assertEquals(1, target.getOperations().size());
        assertEquals(new BigDecimal("5000"), target.getTotal(OperationType.INCOME));
        assertEquals(Map.of("Еда", new BigDecimal("1000")), target.getBudgets());
    }

    @Test
    void returnsIndependentSnapshots() {
        Wallet wallet = walletWithExpenses();
        wallet.setBudget("Еда", new BigDecimal("4000"));

        assertThrows(UnsupportedOperationException.class, () -> wallet.getOperations().clear());
        assertThrows(UnsupportedOperationException.class,
                () -> wallet.getBudgets().put("Такси", BigDecimal.ONE));
    }

    @Test
    void rejectsNegativeBudget() {
        Wallet wallet = new Wallet();

        assertThrows(IllegalArgumentException.class,
                () -> wallet.setBudget("Еда", new BigDecimal("-1")));
    }

    @Test
    void rejectsUnknownBudget() {
        Wallet wallet = new Wallet();

        assertThrows(IllegalArgumentException.class, () -> wallet.getBudgetRemaining("Еда"));
    }

    private Wallet walletWithExpenses() {
        Wallet wallet = new Wallet();
        wallet.addOperation(OperationType.EXPENSE, "Еда", new BigDecimal("300"));
        wallet.addOperation(OperationType.EXPENSE, "Еда", new BigDecimal("500"));
        wallet.addOperation(OperationType.EXPENSE, "Такси", new BigDecimal("1500"));
        return wallet;
    }
}
