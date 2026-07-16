CREATE TABLE transactions
(
    id          UUID PRIMARY KEY,
    account_id  UUID           NOT NULL REFERENCES accounts (id),
    type        VARCHAR(10)    NOT NULL,
    amount      NUMERIC(19, 2) NOT NULL,
    currency    VARCHAR(3)     NOT NULL,
    status      VARCHAR(10)    NOT NULL,
    occurred_at TIMESTAMPTZ    NOT NULL,
    created_at  TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT ck_transactions_amount_positive CHECK (amount > 0),
    CONSTRAINT ck_transactions_type CHECK (type IN ('CREDIT', 'DEBIT')),
    CONSTRAINT ck_transactions_status CHECK (status IN ('SUCCEEDED', 'FAILED'))
);

CREATE INDEX idx_transactions_account_id_occurred_at
    ON transactions (account_id, occurred_at DESC);
