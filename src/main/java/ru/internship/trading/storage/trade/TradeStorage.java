package ru.internship.trading.storage.trade;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import ru.internship.trading.model.Trade;

public interface TradeStorage {
    void record(String code, long buyOrderId, long sellOrderId, BigDecimal price, BigInteger quantity);

    List<Trade> findAll(String code);
}