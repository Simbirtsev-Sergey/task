package ru.internship.trading.storage.instrument;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.jdbc.core.RowMapper;
import java.util.List;
import ru.internship.trading.enums.InstrumentType;
import ru.internship.trading.model.Instrument;

@Repository
public class JdbcInstrumentStorage implements InstrumentStorage {
    private final JdbcTemplate jdbc;

    public JdbcInstrumentStorage(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<Instrument> ROW = (rs, index) -> new Instrument(rs.getString("seccode"),
            InstrumentType.valueOf(rs.getString("type")), rs.getInt("lot_size"),
            rs.getBigDecimal("min_step"), rs.getString("currency"));

    @Override
    public boolean add(Instrument instrument) {
        return jdbc.update("""
                INSERT INTO instruments (seccode,type,lot_size,min_step,currency) VALUES (?,?,?,?,?)
                ON CONFLICT (seccode) DO NOTHING
                """, instrument.seccode(), instrument.type().name(), instrument.lotSize(),
                instrument.minStep(), instrument.currency()) == 1;
    }

    @Override
    public Instrument find(String code) {
        return jdbc.query("SELECT * FROM instruments WHERE seccode = ?", ROW, code).stream().findFirst().orElse(null);
    }

    @Override
    public List<Instrument> findAll() {
        return jdbc.query("SELECT * FROM instruments ORDER BY registration_id", ROW);
    }
}
