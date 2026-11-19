package ru.internship.trading.storage.order;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.jdbc.core.RowMapper;
import java.sql.Timestamp;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import ru.internship.trading.enums.Side;
import ru.internship.trading.enums.Status;
import ru.internship.trading.model.Order;
import ru.internship.trading.model.Level;
import ru.internship.trading.model.Quotes;

@Repository
public class JdbcOrderStorage implements OrderStorage {
    private final JdbcTemplate jdbc;

    public JdbcOrderStorage(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<Order> ROW = (rs, n) -> new Order(rs.getLong("id"), rs.getLong("account_id"),
            rs.getString("seccode"), Side.valueOf(rs.getString("type")), rs.getBigDecimal("price"),
            rs.getBigDecimal("quantity").toBigIntegerExact(), rs.getBigDecimal("filled").toBigIntegerExact(),
            Status.valueOf(rs.getString("status")), rs.getTimestamp("created_at").toInstant(), rs.getLong("priority"));

    @Override
    public void lockTrading() {
        jdbc.execute("LOCK TABLE orders IN SHARE ROW EXCLUSIVE MODE");
    }

    @Override
    public long nextId() {
        return jdbc.queryForObject("SELECT nextval('order_ids')", Long.class);
    }

    @Override
    public long nextPriority() {
        return jdbc.queryForObject("SELECT nextval('order_priorities')", Long.class);
    }

    @Override
    public void save(Order order) {
        jdbc.update("""
                INSERT INTO orders (id,account_id,seccode,type,price,quantity,filled,status,created_at,priority)
                VALUES (?,?,?,?,?,?,?,?,?,?)
                ON CONFLICT (id) DO UPDATE SET price = EXCLUDED.price, quantity = EXCLUDED.quantity,
                    filled = EXCLUDED.filled, status = EXCLUDED.status, priority = EXCLUDED.priority
                """, order.id(), order.accountId(), order.seccode(), order.type().name(), order.price(),
                new BigDecimal(order.quantity()), new BigDecimal(order.filled()), order.status().name(),
                Timestamp.from(order.createdAt()), order.priority());
    }

    @Override
    public Order find(long id) {
        return jdbc.query("SELECT * FROM orders WHERE id = ?", ROW, id).stream().findFirst().orElse(null);
    }

    @Override
    public Order bestCrossing(Order incoming) {
        String priceRule = incoming.type() == Side.BUY
                ? "type = 'SELL' AND price <= ? ORDER BY price ASC, priority"
                : "type = 'BUY' AND price >= ? ORDER BY price DESC, priority";
        return jdbc.query("SELECT * FROM orders WHERE seccode = ? AND status IN ('OPEN','PARTIALLY_FILLED') AND "
                + priceRule + " LIMIT 1", ROW, incoming.seccode(), incoming.price()).stream().findFirst().orElse(null);
    }

    @Override
    public Quotes quotes(String code) {
        List<Level> bids = new ArrayList<>();
        List<Level> asks = new ArrayList<>();
        jdbc.query("""
                SELECT type, price, SUM(quantity - filled) AS units FROM orders
                WHERE seccode = ? AND status IN ('OPEN','PARTIALLY_FILLED')
                GROUP BY type, price ORDER BY type,
                    CASE WHEN type = 'BUY' THEN price END DESC, price ASC
                """, (org.springframework.jdbc.core.RowCallbackHandler) rs -> {
                    var side = Side.BUY.name().equals(rs.getString("type")) ? bids : asks;
                    side.add(new Level(rs.getBigDecimal("price"), rs.getBigDecimal("units").toBigIntegerExact()));
                }, code);
        return new Quotes(code, bids, asks);
    }

    @Override
    public List<Order> findAll(String code, Status status, Long accountId) {
        StringBuilder query = new StringBuilder("SELECT * FROM orders WHERE true");
        List<Object> args = new ArrayList<>();
        if (code != null) {
            query.append(" AND seccode = ?");
            args.add(code);
        }
        if (status != null) {
            query.append(" AND status = ?");
            args.add(status.name());
        }
        if (accountId != null) {
            query.append(" AND account_id = ?");
            args.add(accountId);
        }
        return jdbc.query(query.append(" ORDER BY id").toString(), ROW, args.toArray());
    }
}
