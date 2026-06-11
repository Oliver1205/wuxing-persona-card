CREATE TABLE IF NOT EXISTS user_result (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    result_id VARCHAR(64) UNIQUE NOT NULL,
    birth_year INT NOT NULL,
    birth_month INT NOT NULL,
    birth_day INT NULL,
    birth_time_range VARCHAR(32) NULL,
    answer_json CLOB NOT NULL,
    primary_element VARCHAR(32) NOT NULL,
    secondary_element VARCHAR(32) NOT NULL,
    primary_percent INT NOT NULL,
    secondary_percent INT NOT NULL,
    all_element_scores_json CLOB NOT NULL,
    star_officer_code VARCHAR(64) NOT NULL,
    star_officer_name VARCHAR(64) NOT NULL,
    keywords_json CLOB NOT NULL,
    layout_explanation CLOB NOT NULL,
    strength_text CLOB NOT NULL,
    relationship_text CLOB NOT NULL,
    card_image_key VARCHAR(128) NULL,
    status TINYINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_user_result_created_at ON user_result(created_at);
CREATE INDEX IF NOT EXISTS idx_user_result_primary_secondary ON user_result(primary_element, secondary_element);

CREATE TABLE IF NOT EXISTS short_link (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    short_code VARCHAR(32) UNIQUE NOT NULL,
    result_id VARCHAR(64) NOT NULL,
    original_path VARCHAR(255) NOT NULL,
    short_url VARCHAR(255) NOT NULL,
    pv_count BIGINT NOT NULL DEFAULT 0,
    uv_count BIGINT NOT NULL DEFAULT 0,
    uip_count BIGINT NOT NULL DEFAULT 0,
    last_visit_at TIMESTAMP NULL,
    status TINYINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_short_link_result_id ON short_link(result_id);
CREATE INDEX IF NOT EXISTS idx_short_link_created_at ON short_link(created_at);

CREATE TABLE IF NOT EXISTS visit_event (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    event_type VARCHAR(64) NOT NULL,
    page_path VARCHAR(255) NULL,
    result_id VARCHAR(64) NULL,
    short_code VARCHAR(32) NULL,
    client_id_hash VARCHAR(128) NULL,
    session_id_hash VARCHAR(128) NULL,
    ip_hash VARCHAR(128) NULL,
    user_agent_hash VARCHAR(128) NULL,
    channel VARCHAR(64) NULL,
    campaign VARCHAR(64) NULL,
    device_type VARCHAR(32) NULL,
    referer VARCHAR(512) NULL,
    event_date DATE NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_visit_event_event_created ON visit_event(event_type, created_at);
CREATE INDEX IF NOT EXISTS idx_visit_event_short_created ON visit_event(short_code, created_at);
CREATE INDEX IF NOT EXISTS idx_visit_event_result_created ON visit_event(result_id, created_at);
CREATE INDEX IF NOT EXISTS idx_visit_event_client ON visit_event(client_id_hash);
CREATE INDEX IF NOT EXISTS idx_visit_event_ip ON visit_event(ip_hash);
CREATE INDEX IF NOT EXISTS idx_visit_event_session ON visit_event(session_id_hash);
CREATE INDEX IF NOT EXISTS idx_visit_event_channel_created ON visit_event(channel, created_at);
CREATE INDEX IF NOT EXISTS idx_visit_event_campaign_created ON visit_event(campaign, created_at);
CREATE INDEX IF NOT EXISTS idx_visit_event_event_date ON visit_event(event_date);
