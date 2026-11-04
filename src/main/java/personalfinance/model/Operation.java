package personalfinance.model;

import java.math.BigDecimal;

public record Operation(OperationType type, String category, BigDecimal amount) {
    public Operation {
        if (type == null) {
            throw new IllegalArgumentException("Тип операции не задан");
        }
        if (category == null || category.isBlank()) {
            throw new IllegalArgumentException("Категория не может быть пустой");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Сумма операции должна быть больше нуля");
        }
        category = category.trim();
    }
}
