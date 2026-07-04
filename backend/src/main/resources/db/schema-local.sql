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
    persona_type_id VARCHAR(96) NULL,
    accent_element VARCHAR(32) NULL,
    relation_kind VARCHAR(32) NULL,
    persona_label VARCHAR(96) NULL,
    day_master_text CLOB NULL,
    primary_secondary_text CLOB NULL,
    accent_text CLOB NULL,
    heaven_text CLOB NULL,
    human_text CLOB NULL,
    star_officer_text CLOB NULL,
    growth_advice_json CLOB NULL,
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
CREATE INDEX IF NOT EXISTS idx_user_result_persona_type_id ON user_result(persona_type_id);

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
CREATE INDEX IF NOT EXISTS idx_short_link_status_created_at ON short_link(status, created_at);

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
CREATE INDEX IF NOT EXISTS idx_visit_event_created_at ON visit_event(created_at);
CREATE INDEX IF NOT EXISTS idx_visit_event_created_client ON visit_event(created_at, client_id_hash);
CREATE INDEX IF NOT EXISTS idx_visit_event_created_ip ON visit_event(created_at, ip_hash);
CREATE INDEX IF NOT EXISTS idx_visit_event_event_created_short ON visit_event(event_type, created_at, short_code);
CREATE INDEX IF NOT EXISTS idx_visit_event_client ON visit_event(client_id_hash);
CREATE INDEX IF NOT EXISTS idx_visit_event_ip ON visit_event(ip_hash);
CREATE INDEX IF NOT EXISTS idx_visit_event_session ON visit_event(session_id_hash);
CREATE INDEX IF NOT EXISTS idx_visit_event_channel_created ON visit_event(channel, created_at);
CREATE INDEX IF NOT EXISTS idx_visit_event_campaign_created ON visit_event(campaign, created_at);
CREATE INDEX IF NOT EXISTS idx_visit_event_result_event_channel ON visit_event(result_id, event_type, channel);
CREATE INDEX IF NOT EXISTS idx_visit_event_event_short_created_channel ON visit_event(event_type, short_code, created_at, channel);
CREATE INDEX IF NOT EXISTS idx_visit_event_event_date ON visit_event(event_date);

CREATE TABLE IF NOT EXISTS site_daily_metric (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    metric_date DATE UNIQUE NOT NULL,
    pv BIGINT NOT NULL DEFAULT 0,
    uv BIGINT NOT NULL DEFAULT 0,
    uip BIGINT NOT NULL DEFAULT 0,
    home_views BIGINT NOT NULL DEFAULT 0,
    start_clicks BIGINT NOT NULL DEFAULT 0,
    test_submits BIGINT NOT NULL DEFAULT 0,
    result_created BIGINT NOT NULL DEFAULT 0,
    short_link_created BIGINT NOT NULL DEFAULT 0,
    short_link_visits BIGINT NOT NULL DEFAULT 0,
    aggregated_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_site_daily_metric_aggregated_at ON site_daily_metric(aggregated_at);

CREATE TABLE IF NOT EXISTS short_link_daily_metric (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    metric_date DATE NOT NULL,
    short_code VARCHAR(32) NOT NULL,
    pv BIGINT NOT NULL DEFAULT 0,
    uv BIGINT NOT NULL DEFAULT 0,
    uip BIGINT NOT NULL DEFAULT 0,
    last_visit_at TIMESTAMP NULL,
    aggregated_at TIMESTAMP NOT NULL,
    UNIQUE(metric_date, short_code)
);

CREATE INDEX IF NOT EXISTS idx_short_link_daily_metric_code ON short_link_daily_metric(short_code, metric_date);
CREATE INDEX IF NOT EXISTS idx_short_link_daily_metric_pv ON short_link_daily_metric(metric_date, pv);

CREATE TABLE IF NOT EXISTS analytics_visitor (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    visitor_id_hash VARCHAR(128) UNIQUE NOT NULL,
    first_seen_at TIMESTAMP NOT NULL,
    last_seen_at TIMESTAMP NOT NULL,
    user_agent_hash VARCHAR(128) NULL,
    ip_hash VARCHAR(128) NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_analytics_visitor_last_seen ON analytics_visitor(last_seen_at);

CREATE TABLE IF NOT EXISTS analytics_session (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id_hash VARCHAR(128) UNIQUE NOT NULL,
    visitor_id_hash VARCHAR(128) NOT NULL,
    started_at TIMESTAMP NOT NULL,
    last_heartbeat_at TIMESTAMP NOT NULL,
    ended_at TIMESTAMP NULL,
    entry_path VARCHAR(255) NULL,
    latest_path VARCHAR(255) NULL,
    referrer VARCHAR(512) NULL,
    device_type VARCHAR(32) NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_analytics_session_visitor ON analytics_session(visitor_id_hash);
CREATE INDEX IF NOT EXISTS idx_analytics_session_last_heartbeat ON analytics_session(last_heartbeat_at);
CREATE INDEX IF NOT EXISTS idx_analytics_session_online ON analytics_session(last_heartbeat_at, ended_at);

CREATE TABLE IF NOT EXISTS analytics_metric_snapshot (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    metric_time TIMESTAMP UNIQUE NOT NULL,
    online_visitors BIGINT NOT NULL DEFAULT 0,
    online_sessions BIGINT NOT NULL DEFAULT 0,
    pv_1m BIGINT NOT NULL DEFAULT 0,
    uv_1m BIGINT NOT NULL DEFAULT 0,
    result_generated_1m BIGINT NOT NULL DEFAULT 0,
    share_click_1m BIGINT NOT NULL DEFAULT 0,
    match_enter_1m BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_analytics_metric_time ON analytics_metric_snapshot(metric_time);
