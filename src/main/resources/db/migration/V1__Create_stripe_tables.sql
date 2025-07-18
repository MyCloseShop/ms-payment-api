-- Migration pour créer les tables de paiement Stripe (compatible H2 et MySQL)
-- V1: Création des tables shop_stripe_accounts et stripe_payments

CREATE TABLE shop_stripe_accounts
(
    id                   BINARY(16) PRIMARY KEY,
    shop_id              BINARY(16)   NOT NULL NOT NULL UNIQUE,
    stripe_account_id    VARCHAR(255) NOT NULL UNIQUE,
    onboarding_completed BOOLEAN      NOT NULL DEFAULT FALSE,
    charges_enabled      BOOLEAN      NOT NULL DEFAULT FALSE,
    payouts_enabled      BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP    NULL
);

CREATE INDEX idx_shop_stripe_shop_id ON shop_stripe_accounts (shop_id);
CREATE INDEX idx_shop_stripe_account_id ON shop_stripe_accounts (stripe_account_id);

CREATE TABLE stripe_payments
(
    id                         BINARY(16) PRIMARY KEY,
    stripe_payment_intent_id   VARCHAR(255)   NOT NULL UNIQUE,
    stripe_checkout_session_id VARCHAR(255),
    user_id                    BINARY(16)     NOT NULL,
    shop_id                    BINARY(16)     NOT NULL,
    appointment_id             BINARY(16),
    amount                     DECIMAL(10, 2) NOT NULL,
    commission_amount          DECIMAL(10, 2) NOT NULL,
    commission_rate            DECIMAL(5, 4)  NOT NULL,
    currency                   VARCHAR(3)     NOT NULL DEFAULT 'EUR',
    status                     VARCHAR(50)    NOT NULL DEFAULT 'PENDING',
    refund_eligible_until      TIMESTAMP,
    stripe_refund_id           VARCHAR(255),
    refunded_amount            DECIMAL(10, 2),
    description                TEXT,
    created_at                 TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                 TIMESTAMP      NULL
);

CREATE INDEX idx_stripe_payments_user_id ON stripe_payments (user_id);
CREATE INDEX idx_stripe_payments_shop_id ON stripe_payments (shop_id);
CREATE INDEX idx_stripe_payments_appointment_id ON stripe_payments (appointment_id);
CREATE INDEX idx_stripe_payments_payment_intent ON stripe_payments (stripe_payment_intent_id);
CREATE INDEX idx_stripe_payments_status ON stripe_payments (status);
CREATE INDEX idx_stripe_payments_refund_eligible ON stripe_payments (refund_eligible_until);
