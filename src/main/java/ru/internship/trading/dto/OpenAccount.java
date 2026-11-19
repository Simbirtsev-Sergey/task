package ru.internship.trading.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;

public record OpenAccount(
        @NotBlank @Size(max = 100) String userId,
        @NotBlank String currency,
        @NotNull @PositiveOrZero @Digits(integer = 22, fraction = 8) BigDecimal balance,
        Map<String, @NotNull @PositiveOrZero @Digits(integer = 18, fraction = 0) BigInteger> positions) {}