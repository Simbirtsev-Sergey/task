package ru.internship.trading.storage.account;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import org.springframework.jdbc.core.RowCallbackHandler;
import ru.internship.trading.model.Account;

@Repository
public class JdbcAccountStorage implements AccountStorage {
    private final JdbcTemplate jdbc;

    public JdbcAccountStorage(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Account create(String userId, String currency, BigDecimal balance, Map<String, BigInteger> positions) {
        long id = jdbc.queryForObject("INSERT INTO accounts (user_id,currency,balance) VALUES (?,?,?) RETURNING id",
                Long.class, userId, currency, balance);
        if (positions != null) {
            positions.forEach((code, units) -> jdbc.update(
                    "INSERT INTO positions (account_id,seccode,quantity) VALUES (?,?,?)", id, code, new BigDecimal(units)));
        }
        return find(id);
    }

    @Override
    public Account find(long id) {
        Map<String, BigInteger> positions = new HashMap<>();
        Map<String, BigInteger> reserved = new HashMap<>();
        jdbc.query("""
                SELECT p.seccode, p.quantity, r.units FROM positions p
                LEFT JOIN (
                    SELECT seccode, SUM(quantity - filled) AS units FROM orders
                    WHERE account_id = ? AND type = 'SELL' AND status IN ('OPEN','PARTIALLY_FILLED')
                    GROUP BY seccode
                ) r USING (seccode) WHERE p.account_id = ?
                """, (RowCallbackHandler) rs -> {
                    String code = rs.getString("seccode");
                    positions.put(code, rs.getBigDecimal("quantity").toBigIntegerExact());
                    BigDecimal units = rs.getBigDecimal("units");
                    if (units != null) reserved.put(code, units.toBigIntegerExact());
                }, id, id);
        return jdbc.query("""
                SELECT a.*, COALESCE((
                    SELECT SUM(price * (quantity - filled)) FROM orders
                    WHERE account_id = a.id AND type = 'BUY' AND status IN ('OPEN','PARTIALLY_FILLED')
                ), 0) AS reserved_cash FROM accounts a WHERE id = ?
                """, (rs, n) -> new Account(id, rs.getString("user_id"), rs.getString("currency"),
                        rs.getBigDecimal("balance"), rs.getBigDecimal("reserved_cash"), positions, reserved), id)
                .stream().findFirst().orElse(null);
    }

    @Override
    public boolean sameOwner(long first, long second) {
        return jdbc.queryForObject("""
                SELECT a.user_id = b.user_id FROM accounts a, accounts b WHERE a.id = ? AND b.id = ?
                """, Boolean.class, first, second);
    }

    @Override
    public void transfer(long buyer, long seller, String code, BigDecimal cost, BigInteger units) {
        BigDecimal amount = new BigDecimal(units);
        jdbc.update("UPDATE accounts SET balance = balance - ? WHERE id = ?", cost, buyer);
        jdbc.update("UPDATE accounts SET balance = balance + ? WHERE id = ?", cost, seller);
        jdbc.update("UPDATE positions SET quantity = quantity - ? WHERE account_id = ? AND seccode = ?", amount, seller, code);
        jdbc.update("""
                INSERT INTO positions (account_id,seccode,quantity) VALUES (?,?,?)
                ON CONFLICT (account_id,seccode) DO UPDATE SET quantity = positions.quantity + EXCLUDED.quantity
                """, buyer, code, amount);
    }
}
