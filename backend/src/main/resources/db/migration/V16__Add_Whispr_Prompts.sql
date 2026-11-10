-- Migration V16 : Whispr Prompts (Sondages & Questions Thématiques)

CREATE TABLE IF NOT EXISTS prompts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    question VARCHAR(255) NOT NULL,
    type VARCHAR(20) NOT NULL DEFAULT 'TEXT',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    scale_max INT DEFAULT 10,
    total_responses INT NOT NULL DEFAULT 0,
    rating_sum BIGINT NOT NULL DEFAULT 0,
    rating_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_prompts_user_id ON prompts(user_id);
CREATE INDEX IF NOT EXISTS idx_prompts_user_active ON prompts(user_id, is_active);

CREATE TABLE IF NOT EXISTS prompt_options (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    prompt_id UUID NOT NULL REFERENCES prompts(id) ON DELETE CASCADE,
    option_text VARCHAR(200) NOT NULL,
    vote_count INT NOT NULL DEFAULT 0,
    display_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_prompt_options_prompt_id ON prompt_options(prompt_id);

CREATE TABLE IF NOT EXISTS prompt_answers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    prompt_id UUID NOT NULL REFERENCES prompts(id) ON DELETE CASCADE,
    selected_option_id UUID REFERENCES prompt_options(id) ON DELETE SET NULL,
    rating_value INT,
    text_answer TEXT,
    hashed_ip VARCHAR(128),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_prompt_answers_prompt_ip ON prompt_answers(prompt_id, hashed_ip);
