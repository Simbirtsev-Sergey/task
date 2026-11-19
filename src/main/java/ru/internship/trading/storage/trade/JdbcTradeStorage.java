package ru.internship.trading.storage.trade;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.jdbc.core.RowMapper;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import ru.internship.trading.model.Trade;

@Repository
public class JdbcTradeStorage implements TradeStorage {
    private final JdbcTemplate jdbc;

    public JdbcTradeStorage(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<Trade> ROW = (rs, n) -> new Trade(rs.getLong("id"), rs.getString("seccode"),
            rs.getLong("buy_order_id"), rs.getLong("sell_order_id"), rs.getBigDecimal("price"),
            rs.getBigDecimal("quantity").toBigIntegerExact(), rs.getTimestamp("executed_at").toInstant());

    @Override
    public void record(String code, long buyOrderId, long sellOrderId, BigDecimal price, BigInteger quantity) {
        jdbc.update("INSERT INTO trades (seccode,buy_order_id,sell_order_id,price,quantity) VALUES (?,?,?,?,?)",
                code, buyOrderId, sellOrderId, price, new BigDecimal(quantity));
    }

    @Override
    public List<Trade> findAll(String code) {
        return code == null ? jdbc.query("SELECT * FROM trades ORDER BY id", ROW)
                : jdbc.query("SELECT * FROM trades WHERE seccode = ? ORDER BY id", ROW, code);
    }
}
