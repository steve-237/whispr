-- Migration V9 : Ajout de la ville (city), de la source/referrer dans audit_logs, et table advertisements pour les publicités admin

ALTER TABLE audit_logs ADD COLUMN IF NOT EXISTS city VARCHAR(100) NULL;
ALTER TABLE audit_logs ADD COLUMN IF NOT EXISTS referrer VARCHAR(255) NULL;

CREATE TABLE IF NOT EXISTS advertisements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    image_url VARCHAR(500) NULL,
    target_url VARCHAR(500) NOT NULL,
    cta_text VARCHAR(50) NOT NULL DEFAULT 'En savoir plus',
    badge_text VARCHAR(50) NOT NULL DEFAULT 'Sponsorisé',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    clicks_count INT NOT NULL DEFAULT 0,
    views_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Insertion d'une première publicité Whispr par défaut pour commencer
INSERT INTO advertisements (title, description, target_url, cta_text, badge_text, is_active)
VALUES (
    'Passez à Whispr PRO Club 👑',
    'Débloquez tous les indices secrets sur vos messages anonymes et supprimez toutes les publicités !',
    '/inbox',
    'Découvrir l''offre',
    'Offre Spéciale',
    TRUE
)
ON CONFLICT DO NOTHING;
