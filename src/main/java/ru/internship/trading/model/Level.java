package ru.internship.trading.model;

import java.math.BigDecimal;
import java.math.BigInteger;

public record Level(BigDecimal price, BigInteger quantity) {}