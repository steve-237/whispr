-- Insertion d'un utilisateur de test pour le développement local
INSERT INTO users (id, email, pseudo, password_hash, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000001', 'test@whispr.local', 'demo', 'hashed_password', NOW(), NOW())
ON CONFLICT DO NOTHING;

INSERT INTO profiles (id, user_id, bio, avatar_url, theme_id, daily_question, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000001', 'Bienvenue sur mon profil de test ! Laissez-moi un message anonyme.', NULL, 'default', 'Avez-vous un secret à me confier ?', NOW(), NOW())
ON CONFLICT DO NOTHING;

INSERT INTO links (id, user_id, slug, is_custom, is_active, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000003', '00000000-0000-0000-0000-000000000001', 'demo', false, true, NOW(), NOW())
ON CONFLICT DO NOTHING;
