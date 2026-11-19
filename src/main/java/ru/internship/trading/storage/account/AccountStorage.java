package ru.internship.trading.storage.account;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;
import ru.internship.trading.model.Account;

public interface AccountStorage {
    Account create(String userId, String currency, BigDecimal balance, Map<String, BigInteger> positions);

    Account find(long id);

    boolean sameOwner(long first, long second);

    void transfer(long buyer, long seller, String code, BigDecimal cost, BigInteger units);
}
