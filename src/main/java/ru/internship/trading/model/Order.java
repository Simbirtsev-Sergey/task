package ru.internship.trading.model;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import ru.internship.trading.enums.Side;
import ru.internship.trading.enums.Status;

public record Order(long id, long accountId, String seccode, Side type, BigDecimal price,
                    BigInteger quantity, BigInteger filled, Status status, Instant createdAt,
                    long priority) {
    public BigInteger remaining() {
        return quantity.subtract(filled);
    }

    public Order fill(BigInteger amount) {
        BigInteger executed = filled.add(amount);
        Status next = executed.equals(quantity) ? Status.FILLED : Status.PARTIALLY_FILLED;
        return new Order(id, accountId, seccode, type, price, quantity, executed, next, createdAt, priority);
    }
}