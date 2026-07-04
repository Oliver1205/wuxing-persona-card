-- 五行人格卡实时监控中台迁移脚本
-- 日期：2026-07-03
-- 说明：
-- 1. 只新增 analytics 实时会话与分钟快照表，不删除旧表、不清空旧数据。
-- 2. visitor_id、session_id、IP、UA 均只保存 hash，不保存姓名、出生信息、性别、出生地等敏感信息。
-- 3. 上线执行前请先完成生产库备份，例如：
--    mysqldump -h <host> -u <user> -p --single-transaction --routines --triggers <database> > backup-$(date +%Y%m%d%H%M%S).sql

CREATE TABLE IF NOT EXISTS analytics_visitor (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    visitor_id_hash VARCHAR(128) NOT NULL,
    first_seen_at DATETIME NOT NULL,
    last_seen_at DATETIME NOT NULL,
    user_agent_hash VARCHAR(128) NULL,
    ip_hash VARCHAR(128) NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    UNIQUE KEY uk_analytics_visitor_id(visitor_id_hash),
    INDEX idx_analytics_visitor_last_seen(last_seen_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS analytics_session (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id_hash VARCHAR(128) NOT NULL,
    visitor_id_hash VARCHAR(128) NOT NULL,
    started_at DATETIME NOT NULL,
    last_heartbeat_at DATETIME NOT NULL,
    ended_at DATETIME NULL,
    entry_path VARCHAR(255) NULL,
    latest_path VARCHAR(255) NULL,
    referrer VARCHAR(512) NULL,
    device_type VARCHAR(32) NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    UNIQUE KEY uk_analytics_session_id(session_id_hash),
    INDEX idx_analytics_session_visitor(visitor_id_hash),
    INDEX idx_analytics_session_last_heartbeat(last_heartbeat_at),
    INDEX idx_analytics_session_online(last_heartbeat_at, ended_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS analytics_metric_snapshot (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    metric_time DATETIME NOT NULL,
    online_visitors BIGINT NOT NULL DEFAULT 0,
    online_sessions BIGINT NOT NULL DEFAULT 0,
    pv_1m BIGINT NOT NULL DEFAULT 0,
    uv_1m BIGINT NOT NULL DEFAULT 0,
    result_generated_1m BIGINT NOT NULL DEFAULT 0,
    share_click_1m BIGINT NOT NULL DEFAULT 0,
    match_enter_1m BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    UNIQUE KEY uk_analytics_metric_time(metric_time),
    INDEX idx_analytics_metric_time(metric_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
