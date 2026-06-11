CREATE TABLE IF NOT EXISTS user_result (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    result_id VARCHAR(64) UNIQUE NOT NULL,
    birth_year INT NOT NULL,
    birth_month INT NOT NULL,
    birth_day INT NULL,
    birth_time_range VARCHAR(32) NULL,
    answer_json TEXT NOT NULL,
    primary_element VARCHAR(32) NOT NULL,
    secondary_element VARCHAR(32) NOT NULL,
    primary_percent INT NOT NULL,
    secondary_percent INT NOT NULL,
    all_element_scores_json TEXT NOT NULL,
    star_officer_code VARCHAR(64) NOT NULL,
    star_officer_name VARCHAR(64) NOT NULL,
    keywords_json TEXT NOT NULL,
    layout_explanation TEXT NOT NULL,
    strength_text TEXT NOT NULL,
    relationship_text TEXT NOT NULL,
    card_image_key VARCHAR(128) NULL,
    status TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    UNIQUE KEY uk_result_id(result_id),
    INDEX idx_created_at(created_at),
    INDEX idx_primary_secondary(primary_element, secondary_element)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS short_link (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    short_code VARCHAR(32) UNIQUE NOT NULL,
    result_id VARCHAR(64) NOT NULL,
    original_path VARCHAR(255) NOT NULL,
    short_url VARCHAR(255) NOT NULL,
    pv_count BIGINT NOT NULL DEFAULT 0,
    uv_count BIGINT NOT NULL DEFAULT 0,
    uip_count BIGINT NOT NULL DEFAULT 0,
    last_visit_at DATETIME NULL,
    status TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    UNIQUE KEY uk_short_code(short_code),
    INDEX idx_result_id(result_id),
    INDEX idx_created_at(created_at),
    INDEX idx_status_created_at(status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

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
    created_at DATETIME NOT NULL,
    INDEX idx_event_type_created(event_type, created_at),
    INDEX idx_short_code_created(short_code, created_at),
    INDEX idx_result_id_created(result_id, created_at),
    INDEX idx_created_at(created_at),
    INDEX idx_created_client(created_at, client_id_hash),
    INDEX idx_created_ip(created_at, ip_hash),
    INDEX idx_event_created_short(event_type, created_at, short_code),
    INDEX idx_client_id(client_id_hash),
    INDEX idx_ip_hash(ip_hash),
    INDEX idx_session_id(session_id_hash),
    INDEX idx_channel_created(channel, created_at),
    INDEX idx_campaign_created(campaign, created_at),
    INDEX idx_event_date(event_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE visit_event ADD COLUMN session_id_hash VARCHAR(128) NULL;
ALTER TABLE visit_event ADD COLUMN channel VARCHAR(64) NULL;
ALTER TABLE visit_event ADD COLUMN campaign VARCHAR(64) NULL;
ALTER TABLE visit_event ADD COLUMN device_type VARCHAR(32) NULL;
ALTER TABLE visit_event ADD COLUMN event_date DATE NULL;
CREATE INDEX idx_visit_event_session_id ON visit_event(session_id_hash);
CREATE INDEX idx_visit_event_channel_created ON visit_event(channel, created_at);
CREATE INDEX idx_visit_event_campaign_created ON visit_event(campaign, created_at);
CREATE INDEX idx_visit_event_event_date ON visit_event(event_date);
CREATE INDEX idx_visit_event_created_at ON visit_event(created_at);
CREATE INDEX idx_visit_event_created_client ON visit_event(created_at, client_id_hash);
CREATE INDEX idx_visit_event_created_ip ON visit_event(created_at, ip_hash);
CREATE INDEX idx_visit_event_event_created_short ON visit_event(event_type, created_at, short_code);
CREATE INDEX idx_short_link_status_created_at ON short_link(status, created_at);

CREATE TABLE IF NOT EXISTS site_daily_metric (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    metric_date DATE NOT NULL,
    pv BIGINT NOT NULL DEFAULT 0,
    uv BIGINT NOT NULL DEFAULT 0,
    uip BIGINT NOT NULL DEFAULT 0,
    home_views BIGINT NOT NULL DEFAULT 0,
    start_clicks BIGINT NOT NULL DEFAULT 0,
    test_submits BIGINT NOT NULL DEFAULT 0,
    result_created BIGINT NOT NULL DEFAULT 0,
    short_link_created BIGINT NOT NULL DEFAULT 0,
    short_link_visits BIGINT NOT NULL DEFAULT 0,
    aggregated_at DATETIME NOT NULL,
    UNIQUE KEY uk_site_daily_metric_date(metric_date),
    INDEX idx_site_daily_metric_aggregated_at(aggregated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS short_link_daily_metric (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    metric_date DATE NOT NULL,
    short_code VARCHAR(32) NOT NULL,
    pv BIGINT NOT NULL DEFAULT 0,
    uv BIGINT NOT NULL DEFAULT 0,
    uip BIGINT NOT NULL DEFAULT 0,
    last_visit_at DATETIME NULL,
    aggregated_at DATETIME NOT NULL,
    UNIQUE KEY uk_short_link_daily_metric(metric_date, short_code),
    INDEX idx_short_link_daily_metric_code(short_code, metric_date),
    INDEX idx_short_link_daily_metric_pv(metric_date, pv)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
