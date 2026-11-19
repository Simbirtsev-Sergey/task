package ru.internship.trading.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.math.BigInteger;
import ru.internship.trading.enums.Side;

public record NewOrder(@Positive long accountId, @NotBlank String seccode, @NotNull Side type,
                       @NotNull @Positive @Digits(integer = 12, fraction = 8) BigDecimal price,
                       @NotNull @Positive @Digits(integer = 18, fraction = 0) BigInteger quantity) {}