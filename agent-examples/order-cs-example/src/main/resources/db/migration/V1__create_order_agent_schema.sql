CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_no VARCHAR(64) NOT NULL UNIQUE,
    nickname VARCHAR(128) NOT NULL,
    level_name VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE orders (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_no VARCHAR(64) NOT NULL UNIQUE,
    user_no VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    amount DECIMAL(12, 2) NOT NULL,
    paid_at TIMESTAMP NULL,
    completed_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_orders_user_no (user_no)
);

CREATE TABLE payments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_no VARCHAR(64) NOT NULL,
    channel VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    callback_status VARCHAR(32) NOT NULL,
    paid_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_payments_order_no (order_no)
);

CREATE TABLE refunds (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_no VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    reason VARCHAR(255) NULL,
    requested_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_refunds_order_no (order_no)
);

CREATE TABLE benefits (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_no VARCHAR(64) NOT NULL,
    order_no VARCHAR(64) NOT NULL,
    benefit_name VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL,
    fail_reason VARCHAR(255) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_benefits_user_no (user_no),
    INDEX idx_benefits_order_no (order_no)
);

CREATE TABLE agent_sessions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    trace_id VARCHAR(64) NOT NULL,
    question TEXT NOT NULL,
    answer TEXT NULL,
    duration_ms BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_agent_sessions_trace_id (trace_id)
);

CREATE TABLE agent_tool_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    trace_id VARCHAR(64) NOT NULL,
    tool_name VARCHAR(128) NOT NULL,
    input_json TEXT NOT NULL,
    output_summary TEXT NULL,
    duration_ms BIGINT NOT NULL,
    success TINYINT(1) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_agent_tool_logs_trace_id (trace_id)
);

CREATE TABLE rag_hit_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    trace_id VARCHAR(64) NOT NULL,
    source VARCHAR(255) NOT NULL,
    score DECIMAL(12, 8) NOT NULL,
    content_preview VARCHAR(512) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_rag_hit_logs_trace_id (trace_id)
);
