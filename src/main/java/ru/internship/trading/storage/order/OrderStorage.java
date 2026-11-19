package ru.internship.trading.storage.order;

import java.util.List;
import ru.internship.trading.enums.Status;
import ru.internship.trading.model.Order;
import ru.internship.trading.model.Quotes;

public interface OrderStorage {
    void lockTrading();

    long nextId();

    long nextPriority();

    Order bestCrossing(Order incoming);

    Quotes quotes(String code);

    void save(Order order);

    Order find(long id);

    List<Order> findAll(String code, Status status, Long accountId);
}
