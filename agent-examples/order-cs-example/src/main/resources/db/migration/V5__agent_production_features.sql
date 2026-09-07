CREATE TABLE knowledge_chunks (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    doc_code VARCHAR(64) NOT NULL,
    chunk_index INT NOT NULL,
    vector_point_id VARCHAR(64) NOT NULL,
    content TEXT NOT NULL,
    article_version INT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_knowledge_chunks_doc_chunk (doc_code, chunk_index),
    UNIQUE KEY uk_knowledge_chunks_point (vector_point_id),
    INDEX idx_knowledge_chunks_doc_code (doc_code)
);

CREATE TABLE agent_conversations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id VARCHAR(64) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE agent_conversation_turns (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id VARCHAR(64) NOT NULL,
    turn_index INT NOT NULL,
    trace_id VARCHAR(64) NOT NULL,
    role VARCHAR(16) NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_agent_turns_session_index (session_id, turn_index),
    INDEX idx_agent_turns_session_id (session_id)
);

CREATE TABLE agent_action_requests (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    action_id VARCHAR(64) NOT NULL UNIQUE,
    session_id VARCHAR(64) NULL,
    trace_id VARCHAR(64) NOT NULL,
    action_type VARCHAR(64) NOT NULL,
    order_no VARCHAR(64) NOT NULL,
    payload_json TEXT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    reason TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_agent_actions_session (session_id),
    INDEX idx_agent_actions_status (status)
);

CREATE TABLE rag_eval_cases (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    question TEXT NOT NULL,
    expected_doc_code VARCHAR(64) NOT NULL,
    category VARCHAR(64) NULL,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE agent_sessions
    ADD COLUMN session_id VARCHAR(64) NULL AFTER trace_id,
    ADD COLUMN turn_index INT NOT NULL DEFAULT 0 AFTER session_id,
    ADD INDEX idx_agent_sessions_session_id (session_id);

INSERT INTO rag_eval_cases (question, expected_doc_code, category) VALUES
('订单为什么不能退款？', 'refund-policy', 'refund'),
('超过7天还能退款吗？', 'refund-policy', 'refund'),
('支付成功但订单状态不对怎么办？', 'payment-troubleshooting', 'payment'),
('支付回调失败怎么处理？', 'payment-troubleshooting', 'payment'),
('会员权益为什么没到账？', 'member-benefits', 'benefit'),
('权益发放失败可以重试吗？', 'member-benefits', 'benefit');
