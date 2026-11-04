package personalfinance;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Wallet {
    private final List<Operation> operations = new ArrayList<>();
    private final Map<String, BigDecimal> budgets = new LinkedHashMap<>();

    public void addOperation(OperationType type, String category, BigDecimal amount) {
        operations.add(new Operation(type, category, amount));
    }

    public void setBudget(String category, BigDecimal limit) {
        if (category == null || category.isBlank()) {
            throw new IllegalArgumentException("Категория не может быть пустой");
        }
        if (limit == null || limit.signum() < 0) {
            throw new IllegalArgumentException("Бюджет не может быть отрицательным");
        }
        budgets.put(category.trim(), limit);
    }

    public List<Operation> getOperations() {
        return List.copyOf(operations);
    }

    public Map<String, BigDecimal> getBudgets() {
        return Map.copyOf(budgets);
    }

    public BigDecimal getTotal(OperationType type) {
        BigDecimal total = BigDecimal.ZERO;
        for (Operation operation : operations) {
            if (operation.type() == type) {
                total = total.add(operation.amount());
            }
        }
        return total;
    }

    public BigDecimal getTotalByCategory(OperationType type, String category) {
        BigDecimal total = BigDecimal.ZERO;
        for (Operation operation : operations) {
            if (operation.type() == type && operation.category().equals(category)) {
                total = total.add(operation.amount());
            }
        }
        return total;
    }

    public BigDecimal getTotalByCategories(OperationType type, Set<String> categories) {
        BigDecimal total = BigDecimal.ZERO;
        for (Operation operation : operations) {
            if (operation.type() == type && categories.contains(operation.category())) {
                total = total.add(operation.amount());
            }
        }
        return total;
    }

    public Map<String, BigDecimal> getTotalsByCategory(OperationType type) {
        Map<String, BigDecimal> totals = new LinkedHashMap<>();
        for (Operation operation : operations) {
            if (operation.type() == type) {
                BigDecimal current = totals.getOrDefault(operation.category(), BigDecimal.ZERO);
                totals.put(operation.category(), current.add(operation.amount()));
            }
        }
        return totals;
    }

    public BigDecimal getBudgetRemaining(String category) {
        BigDecimal budget = budgets.get(category);
        if (budget == null) {
            throw new IllegalArgumentException("Бюджет для категории не установлен: " + category);
        }
        return budget.subtract(getTotalByCategory(OperationType.EXPENSE, category));
    }
}
