package ru.internship.trading.model;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;

public record Trade(long id, String seccode, long buyOrderId, long sellOrderId,
                    BigDecimal price, BigInteger quantity, Instant executedAt) {}