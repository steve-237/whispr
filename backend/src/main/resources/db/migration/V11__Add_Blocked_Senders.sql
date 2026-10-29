-- Migration V11 : Système de Shadowban / Blocage d'expéditeurs malveillants

CREATE TABLE IF NOT EXISTS blocked_senders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    blocked_hashed_ip VARCHAR(64) NOT NULL,
    reason VARCHAR(255) DEFAULT 'Expéditeur bloqué par le destinataire',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_user_blocked_ip ON blocked_senders(user_id, blocked_hashed_ip);
CREATE INDEX IF NOT EXISTS idx_blocked_senders_user ON blocked_senders(user_id);
