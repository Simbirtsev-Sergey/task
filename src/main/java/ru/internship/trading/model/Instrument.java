package ru.internship.trading.model;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import ru.internship.trading.enums.InstrumentType;

public record Instrument(
        @NotBlank String seccode,
        @NotNull InstrumentType type,
        @Positive int lotSize,
        @NotNull @Positive @Digits(integer = 12, fraction = 8) BigDecimal minStep,
        @NotBlank String currency) {}