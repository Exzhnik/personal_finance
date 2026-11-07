package personalfinance.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OperationTest {
    @Test
    void createsOperationAndTrimsCategory() {
        Operation operation = new Operation(OperationType.EXPENSE, "  Еда  ", new BigDecimal("100.50"));

        assertEquals(OperationType.EXPENSE, operation.type());
        assertEquals("Еда", operation.category());
        assertEquals(new BigDecimal("100.50"), operation.amount());
    }

    @Test
    void rejectsNullType() {
        assertThrows(IllegalArgumentException.class,
                () -> new Operation(null, "Еда", BigDecimal.ONE));
    }

    @Test
    void rejectsBlankCategory() {
        assertThrows(IllegalArgumentException.class,
                () -> new Operation(OperationType.EXPENSE, "  ", BigDecimal.ONE));
    }

    @Test
    void rejectsNullAmount() {
        assertThrows(IllegalArgumentException.class,
                () -> new Operation(OperationType.EXPENSE, "Еда", null));
    }

    @Test
    void rejectsZeroAmount() {
        assertThrows(IllegalArgumentException.class,
                () -> new Operation(OperationType.EXPENSE, "Еда", BigDecimal.ZERO));
    }

    @Test
    void rejectsNegativeAmount() {
        assertThrows(IllegalArgumentException.class,
                () -> new Operation(OperationType.EXPENSE, "Еда", new BigDecimal("-1")));
    }
}
