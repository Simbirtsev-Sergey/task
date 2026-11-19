package ru.internship.trading.storage.instrument;

import java.util.List;
import ru.internship.trading.model.Instrument;

public interface InstrumentStorage {
    boolean add(Instrument instrument);

    Instrument find(String code);

    List<Instrument> findAll();
}
