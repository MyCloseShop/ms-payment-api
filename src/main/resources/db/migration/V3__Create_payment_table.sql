-- Migration pour créer la table payment optimisée pour Stripe Connect
CREATE TABLE payment (
  id BINARY(16) PRIMARY KEY,
  amount BIGINT NOT NULL,
  currency VARCHAR(3) NOT NULL,
  user_id BINARY(16) NOT NULL,
  shop_id BINARY(16) NOT NULL,
  appointment_id BINARY(16),
  status VARCHAR(20) NOT NULL,
  stripe_session_id VARCHAR(255),
  stripe_payment_intent_id VARCHAR(255),
  stripe_charge_id VARCHAR(255),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Création des index séparément pour compatibilité H2/MySQL
CREATE INDEX idx_stripe_session_id ON payment(stripe_session_id);
CREATE INDEX idx_stripe_payment_intent_id ON payment(stripe_payment_intent_id);
CREATE INDEX idx_user_shop ON payment(user_id, shop_id);
CREATE INDEX idx_appointment_id ON payment(appointment_id);
