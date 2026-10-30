-- Support des pourboires visiteurs (Tips) via Stripe
ALTER TABLE messages ADD COLUMN IF NOT EXISTS tip_amount_eur NUMERIC(10, 2) DEFAULT NULL;
ALTER TABLE messages ADD COLUMN IF NOT EXISTS is_tip_paid BOOLEAN DEFAULT FALSE;
ALTER TABLE messages ADD COLUMN IF NOT EXISTS stripe_tip_session_id VARCHAR(255) DEFAULT NULL;

CREATE INDEX IF NOT EXISTS idx_messages_stripe_tip_session ON messages(stripe_tip_session_id);
