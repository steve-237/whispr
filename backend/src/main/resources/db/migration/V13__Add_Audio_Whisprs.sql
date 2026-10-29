-- Support des Whisprs Audio et filtres vocaux déformés
ALTER TABLE messages ADD COLUMN IF NOT EXISTS audio_url TEXT;
ALTER TABLE messages ADD COLUMN IF NOT EXISTS audio_duration_seconds INT;
ALTER TABLE messages ADD COLUMN IF NOT EXISTS voice_filter VARCHAR(50);
