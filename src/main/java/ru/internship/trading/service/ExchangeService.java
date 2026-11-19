package ru.internship.trading.service;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ru.internship.trading.dto.Amendment;
import ru.internship.trading.dto.NewOrder;
import ru.internship.trading.dto.OpenAccount;
import ru.internship.trading.enums.Side;
import ru.internship.trading.enums.Status;
import ru.internship.trading.model.Account;
import ru.internship.trading.model.Instrument;
import ru.internship.trading.model.Order;
import ru.internship.trading.model.Quotes;
import ru.internship.trading.model.Trade;
import ru.internship.trading.storage.account.AccountStorage;
import ru.internship.trading.storage.instrument.InstrumentStorage;
import ru.internship.trading.storage.order.OrderStorage;
import ru.internship.trading.storage.trade.TradeStorage;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
public class ExchangeService {
    private final InstrumentStorage instruments;
    private final AccountStorage accounts;
    private final OrderStorage orders;
    private final TradeStorage trades;

    public ExchangeService(InstrumentStorage instruments, AccountStorage accounts, OrderStorage orders, TradeStorage trades) {
        this.instruments = instruments;
        this.accounts = accounts;
        this.orders = orders;
        this.trades = trades;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Instrument add(Instrument instrument) {
        if (!instruments.add(instrument)) {
            throw new ResponseStatusException(CONFLICT, "Instrument already exists");
        }
        return instrument;
    }

    public List<Instrument> instruments() {
        return instruments.findAll();
    }

    private Instrument instrument(String code) {
        Instrument result = instruments.find(code);
        if (result == null) throw new ResponseStatusException(NOT_FOUND, "Unknown instrument");
        return result;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Account open(OpenAccount request) {
        if (request.positions() != null) {
            for (String code : request.positions().keySet()) {
                checkCurrency(request.currency(), instrument(code));
            }
        }
        Account account = accounts.create(request.userId(), request.currency(), request.balance(), request.positions());
        return account;
    }

    private Account owned(long id, String user) {
        Account account = accounts.find(id);
        if (account == null) throw new ResponseStatusException(NOT_FOUND, "Unknown account");
        if (!account.userId().equals(user)) throw new ResponseStatusException(FORBIDDEN, "Account belongs to another user");
        return account;
    }

    public Account account(long id, String user) {
        return owned(id, user);
    }

    private void checkCurrency(String currency, Instrument instrument) {
        if (!currency.equals(instrument.currency())) {
            throw new ResponseStatusException(BAD_REQUEST, "Account and settlement currencies must match");
        }
    }

    private void validate(Account account, Instrument instrument, Side side, BigDecimal price,
                          BigInteger remaining, Order replaced) {
        checkCurrency(account.currency(), instrument);
        if (price.remainder(instrument.minStep()).signum() != 0) {
            throw new ResponseStatusException(BAD_REQUEST, "Price must be a multiple of min_step");
        }
        if (remaining.signum() <= 0 || remaining.mod(BigInteger.valueOf(instrument.lotSize())).signum() != 0) {
            throw new ResponseStatusException(BAD_REQUEST, "Remaining quantity must be a positive multiple of lot_size");
        }
        BigInteger released = replaced == null ? BigInteger.ZERO : replaced.remaining();
        boolean covered;
        if (side == Side.BUY) {
            BigDecimal available = account.balance().subtract(account.reservedCash());
            if (replaced != null) available = available.add(replaced.price().multiply(new BigDecimal(released)));
            covered = available.compareTo(price.multiply(new BigDecimal(remaining))) >= 0;
        } else {
            String code = instrument.seccode();
            BigInteger available = account.positions().getOrDefault(code, BigInteger.ZERO)
                    .subtract(account.reservedPositions().getOrDefault(code, BigInteger.ZERO)).add(released);
            covered = available.compareTo(remaining) >= 0;
        }
        if (!covered) {
            throw new ResponseStatusException(CONFLICT, "Insufficient available cash or position");
        }
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Order place(String user, NewOrder input) {
        orders.lockTrading();
        Account account = owned(input.accountId(), user);
        Instrument instrument = instrument(input.seccode());
        validate(account, instrument, input.type(), input.price(), input.quantity(), null);
        return enter(new Order(orders.nextId(), account.id(), input.seccode(), input.type(), input.price(),
                input.quantity(), BigInteger.ZERO, Status.OPEN, Instant.now(), orders.nextPriority()));
    }

    private Order editable(String user, long id) {
        Order current = orders.find(id);
        if (current == null) throw new ResponseStatusException(NOT_FOUND, "Unknown order");
        owned(current.accountId(), user);
        if (current.status() == Status.FILLED || current.status() == Status.CANCELLED) {
            throw new ResponseStatusException(CONFLICT, "Order is closed");
        }
        return current;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Order amend(String user, long id, Amendment input) {
        orders.lockTrading();
        Order old = editable(user, id);
        Account account = accounts.find(old.accountId());
        validate(account, instrument(old.seccode()), old.type(), input.price(),
                input.quantity().subtract(old.filled()), old);
        return enter(new Order(old.id(), old.accountId(), old.seccode(), old.type(), input.price(),
                input.quantity(), old.filled(), old.status(), old.createdAt(), orders.nextPriority()));
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Order cancel(String user, long id) {
        orders.lockTrading();
        Order order = editable(user, id);
        return close(order);
    }

    private Order close(Order order) {
        Order closed = new Order(order.id(), order.accountId(), order.seccode(), order.type(), order.price(),
                order.quantity(), order.filled(), Status.CANCELLED, order.createdAt(), order.priority());
        orders.save(closed);
        return closed;
    }

    private Order enter(Order incoming) {
        orders.save(incoming);
        while (incoming.remaining().signum() > 0) {
            Order maker = orders.bestCrossing(incoming);
            if (maker == null) break;
            if (accounts.sameOwner(incoming.accountId(), maker.accountId())) {
                return close(incoming);
            }
            BigInteger amount = incoming.remaining().min(maker.remaining());
            settle(incoming, maker, amount);
            orders.save(maker.fill(amount));
            incoming = incoming.fill(amount);
        }
        if (incoming.filled().signum() > 0) orders.save(incoming);
        return incoming;
    }

    private void settle(Order taker, Order maker, BigInteger amount) {
        Order buy = taker.type() == Side.BUY ? taker : maker;
        Order sell = taker.type() == Side.SELL ? taker : maker;
        BigDecimal cost = maker.price().multiply(new BigDecimal(amount));
        accounts.transfer(buy.accountId(), sell.accountId(), maker.seccode(), cost, amount);
        trades.record(maker.seccode(), buy.id(), sell.id(), maker.price(), amount);
    }

    public Quotes quotes(String code) {
        instrument(code);
        return orders.quotes(code);
    }

    public List<Order> orders(String code, Status status, Long accountId) {
        return orders.findAll(code, status, accountId);
    }

    public List<Trade> trades(String code) {
        return trades.findAll(code);
    }
}
