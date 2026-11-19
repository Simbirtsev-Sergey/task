CREATE TABLE IF NOT EXISTS instruments (
    seccode text PRIMARY KEY,
    type text NOT NULL CHECK (type IN ('STOCK','BOND','FX')),
    lot_size integer NOT NULL CHECK (lot_size > 0),
    min_step numeric NOT NULL CHECK (min_step > 0),
    currency text NOT NULL,
    registration_id bigint GENERATED ALWAYS AS IDENTITY UNIQUE
);

CREATE TABLE IF NOT EXISTS accounts (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id text NOT NULL,
    currency text NOT NULL,
    balance numeric NOT NULL CHECK (balance >= 0)
);

CREATE TABLE IF NOT EXISTS positions (
    account_id bigint REFERENCES accounts NOT NULL,
    seccode text REFERENCES instruments NOT NULL,
    quantity numeric NOT NULL CHECK (quantity >= 0 AND quantity = trunc(quantity)),
    PRIMARY KEY (account_id, seccode)
);

CREATE SEQUENCE IF NOT EXISTS order_ids;
CREATE SEQUENCE IF NOT EXISTS order_priorities;
CREATE TABLE IF NOT EXISTS orders (
    id bigint PRIMARY KEY,
    account_id bigint NOT NULL REFERENCES accounts,
    seccode text NOT NULL REFERENCES instruments,
    type text NOT NULL CHECK (type IN ('BUY','SELL')),
    price numeric NOT NULL CHECK (price > 0),
    quantity numeric NOT NULL CHECK (quantity > 0 AND quantity = trunc(quantity)),
    filled numeric NOT NULL CHECK (filled BETWEEN 0 AND quantity AND filled = trunc(filled)),
    status text NOT NULL CHECK (status IN ('OPEN','PARTIALLY_FILLED','FILLED','CANCELLED')),
    created_at timestamptz NOT NULL,
    priority bigint UNIQUE NOT NULL
);

CREATE INDEX IF NOT EXISTS orders_asks ON orders (seccode, price, priority)
    WHERE type = 'SELL' AND status IN ('OPEN','PARTIALLY_FILLED');
CREATE INDEX IF NOT EXISTS orders_bids ON orders (seccode, price DESC, priority)
    WHERE type = 'BUY' AND status IN ('OPEN','PARTIALLY_FILLED');
CREATE INDEX IF NOT EXISTS orders_reserved ON orders (account_id, type, seccode)
    WHERE status IN ('OPEN','PARTIALLY_FILLED');

CREATE TABLE IF NOT EXISTS trades (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    seccode text NOT NULL REFERENCES instruments,
    buy_order_id bigint NOT NULL REFERENCES orders,
    sell_order_id bigint NOT NULL REFERENCES orders,
    price numeric NOT NULL CHECK (price > 0),
    quantity numeric NOT NULL CHECK (quantity > 0 AND quantity = trunc(quantity)),
    executed_at timestamptz NOT NULL DEFAULT clock_timestamp()
);
CREATE INDEX IF NOT EXISTS trades_by_instrument ON trades (seccode, id);

-- Widen currency columns created by earlier versions.
ALTER TABLE instruments ALTER COLUMN currency TYPE text;
ALTER TABLE accounts ALTER COLUMN currency TYPE text;
