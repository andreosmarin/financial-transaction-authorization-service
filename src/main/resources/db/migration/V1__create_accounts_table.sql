CREATE TABLE accounts
(
    id         UUID PRIMARY KEY,
    owner_id   UUID           NOT NULL,
    balance    NUMERIC(19, 2) NOT NULL DEFAULT 0.00,
    currency   VARCHAR(3)     NOT NULL DEFAULT 'BRL',
    status     VARCHAR(20)    NOT NULL,
    opened_at  TIMESTAMPTZ    NOT NULL,
    created_at TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT ck_accounts_balance_non_negative
        CHECK (balance >= 0)
);