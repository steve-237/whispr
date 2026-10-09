-- Migration V7 : Monétisation, Portefeuille de pièces virtuelles, Statut PRO, Transactions et Indices débloqués

CREATE TABLE IF NOT EXISTS wallets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    coins INT NOT NULL DEFAULT 30,
    total_spent_eur DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    is_pro BOOLEAN NOT NULL DEFAULT FALSE,
    pro_expires_at TIMESTAMP WITH TIME ZONE NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type VARCHAR(50) NOT NULL, -- 'COIN_PURCHASE', 'PRO_SUBSCRIPTION', 'CLUE_UNLOCK', 'SUPER_WHISPR'
    amount_eur DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    coins_delta INT NOT NULL DEFAULT 0,
    description VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS unlocked_clues (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    message_id UUID NOT NULL REFERENCES messages(id) ON DELETE CASCADE,
    clue_type VARCHAR(50) NOT NULL, -- 'LOCATION', 'DEVICE', 'NETWORK'
    clue_value VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_user_message_clue UNIQUE (user_id, message_id, clue_type)
);

-- Initialisation d'un portefeuille avec 30 pièces offertes pour chaque utilisateur existant
INSERT INTO wallets (user_id, coins, total_spent_eur, is_pro)
SELECT id, 30, 0.00, FALSE FROM users
ON CONFLICT (user_id) DO NOTHING;
