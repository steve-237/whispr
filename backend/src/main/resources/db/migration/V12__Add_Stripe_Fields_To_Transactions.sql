-- Ajout du support de Stripe Checkout et Webhooks idempotents pour les transactions
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS stripe_session_id VARCHAR(255);
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS stripe_event_id VARCHAR(255);
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS status VARCHAR(50) DEFAULT 'COMPLETED';

CREATE INDEX IF NOT EXISTS idx_transactions_stripe_session_id ON transactions(stripe_session_id);
CREATE INDEX IF NOT EXISTS idx_transactions_stripe_event_id ON transactions(stripe_event_id);
