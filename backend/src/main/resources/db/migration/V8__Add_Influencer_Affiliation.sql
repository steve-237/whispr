-- Migration V8 : Programme Affiliation / Créateurs & Influenceurs
ALTER TABLE wallets ADD COLUMN IF NOT EXISTS earnings_eur DECIMAL(10,2) NOT NULL DEFAULT 0.00;
ALTER TABLE wallets ADD COLUMN IF NOT EXISTS referral_count INT NOT NULL DEFAULT 0;
ALTER TABLE wallets ADD COLUMN IF NOT EXISTS affiliate_code VARCHAR(50) NULL;

-- Initialiser le code d'affiliation avec le pseudo de l'utilisateur
UPDATE wallets w
SET affiliate_code = u.pseudo
FROM users u
WHERE w.user_id = u.id AND w.affiliate_code IS NULL;
