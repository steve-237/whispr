ALTER TABLE wallets ADD COLUMN IF NOT EXISTS stripe_customer_id VARCHAR(100);
ALTER TABLE wallets ADD COLUMN IF NOT EXISTS stripe_subscription_id VARCHAR(100);
ALTER TABLE wallets ADD COLUMN IF NOT EXISTS subscription_status VARCHAR(50);

CREATE INDEX IF NOT EXISTS idx_wallets_stripe_sub ON wallets(stripe_subscription_id);
CREATE INDEX IF NOT EXISTS idx_wallets_stripe_cust ON wallets(stripe_customer_id);
