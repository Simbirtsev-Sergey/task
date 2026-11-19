package ru.internship.trading.model;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;

public record Account(long id, String userId, String currency, BigDecimal balance,
                      BigDecimal reservedCash, Map<String, BigInteger> positions,
                      Map<String, BigInteger> reservedPositions) {}