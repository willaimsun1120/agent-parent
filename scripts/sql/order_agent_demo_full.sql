-- ======================================================================
-- AgentForge - 订单客服 Demo 完整建库脚本
-- Database: order_agent_demo
-- Generated from Flyway migrations: order-cs-example
-- Migrations: V1__create_order_agent_schema.sql, V2__insert_demo_data.sql, V3__add_table_and_column_comments.sql, V4__knowledge_articles.sql, V5__agent_production_features.sql, V6__add_v4_v5_table_comments.sql, V7__fix_conversation_turn_unique_key.sql, V8__knowledge_articles_zh_content.sql, V9__agent_prompt_templates.sql, V10__prompt_code_structure.sql, V11__agent_conversation_session_state.sql
-- ======================================================================

CREATE DATABASE IF NOT EXISTS `order_agent_demo` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `order_agent_demo`;

-- ----------------------------------------------------------------------
-- Source: V1__create_order_agent_schema.sql
-- ----------------------------------------------------------------------
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


-- ----------------------------------------------------------------------
-- Source: V2__insert_demo_data.sql
-- ----------------------------------------------------------------------
INSERT INTO users (user_no, nickname, level_name) VALUES
('U1001', 'Alice', 'SVIP'),
('U1002', 'Bob', 'VIP'),
('U1003', 'Cindy', 'Normal');

INSERT INTO orders (order_no, user_no, status, amount, paid_at, completed_at) VALUES
('ORD-1001', 'U1001', 'COMPLETED', 199.00, '2026-06-01 10:00:00', '2026-06-01 10:10:00'),
('ORD-1002', 'U1002', 'PENDING_PAYMENT_CALLBACK', 59.00, '2026-06-08 09:00:00', NULL),
('ORD-1003', 'U1003', 'BENEFIT_FAILED', 99.00, '2026-06-08 11:30:00', '2026-06-08 11:45:00');

INSERT INTO payments (order_no, channel, status, callback_status, paid_at) VALUES
('ORD-1001', 'ALIPAY', 'SUCCESS', 'SUCCESS', '2026-06-01 10:00:00'),
('ORD-1002', 'WECHAT', 'SUCCESS', 'FAILED', '2026-06-08 09:00:00'),
('ORD-1003', 'ALIPAY', 'SUCCESS', 'SUCCESS', '2026-06-08 11:30:00');

INSERT INTO refunds (order_no, status, reason, requested_at) VALUES
('ORD-1001', 'REJECTED', 'Order completed more than 7 days ago.', '2026-06-08 12:00:00');

INSERT INTO benefits (user_no, order_no, benefit_name, status, fail_reason) VALUES
('U1001', 'ORD-1001', 'SVIP monthly bonus', 'ISSUED', NULL),
('U1002', 'ORD-1002', 'VIP coupon package', 'PENDING', NULL),
('U1003', 'ORD-1003', 'New user benefit', 'FAILED', 'Benefit service timeout.');


-- ----------------------------------------------------------------------
-- Source: V3__add_table_and_column_comments.sql
-- ----------------------------------------------------------------------
ALTER TABLE users COMMENT = '业务用户表';
ALTER TABLE users MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID';
ALTER TABLE users MODIFY COLUMN user_no VARCHAR(64) NOT NULL COMMENT '业务用户编号';
ALTER TABLE users MODIFY COLUMN nickname VARCHAR(128) NOT NULL COMMENT '用户昵称';
ALTER TABLE users MODIFY COLUMN level_name VARCHAR(64) NOT NULL COMMENT '会员等级名称';
ALTER TABLE users MODIFY COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';

ALTER TABLE orders COMMENT = '订单主表';
ALTER TABLE orders MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID';
ALTER TABLE orders MODIFY COLUMN order_no VARCHAR(64) NOT NULL COMMENT '业务订单号';
ALTER TABLE orders MODIFY COLUMN user_no VARCHAR(64) NOT NULL COMMENT '业务用户编号';
ALTER TABLE orders MODIFY COLUMN status VARCHAR(32) NOT NULL COMMENT '订单状态';
ALTER TABLE orders MODIFY COLUMN amount DECIMAL(12, 2) NOT NULL COMMENT '订单金额';
ALTER TABLE orders MODIFY COLUMN paid_at TIMESTAMP NULL COMMENT '支付成功时间';
ALTER TABLE orders MODIFY COLUMN completed_at TIMESTAMP NULL COMMENT '订单完成时间';
ALTER TABLE orders MODIFY COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';

ALTER TABLE payments COMMENT = '支付记录表';
ALTER TABLE payments MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID';
ALTER TABLE payments MODIFY COLUMN order_no VARCHAR(64) NOT NULL COMMENT '业务订单号';
ALTER TABLE payments MODIFY COLUMN channel VARCHAR(32) NOT NULL COMMENT '支付渠道';
ALTER TABLE payments MODIFY COLUMN status VARCHAR(32) NOT NULL COMMENT '支付状态';
ALTER TABLE payments MODIFY COLUMN callback_status VARCHAR(32) NOT NULL COMMENT '支付回调状态';
ALTER TABLE payments MODIFY COLUMN paid_at TIMESTAMP NULL COMMENT '支付成功时间';
ALTER TABLE payments MODIFY COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';

ALTER TABLE refunds COMMENT = '退款记录表';
ALTER TABLE refunds MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID';
ALTER TABLE refunds MODIFY COLUMN order_no VARCHAR(64) NOT NULL COMMENT '业务订单号';
ALTER TABLE refunds MODIFY COLUMN status VARCHAR(32) NOT NULL COMMENT '退款状态';
ALTER TABLE refunds MODIFY COLUMN reason VARCHAR(255) NULL COMMENT '退款原因或拒绝原因';
ALTER TABLE refunds MODIFY COLUMN requested_at TIMESTAMP NULL COMMENT '退款申请时间';
ALTER TABLE refunds MODIFY COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';

ALTER TABLE benefits COMMENT = '会员权益发放记录表';
ALTER TABLE benefits MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID';
ALTER TABLE benefits MODIFY COLUMN user_no VARCHAR(64) NOT NULL COMMENT '业务用户编号';
ALTER TABLE benefits MODIFY COLUMN order_no VARCHAR(64) NOT NULL COMMENT '业务订单号';
ALTER TABLE benefits MODIFY COLUMN benefit_name VARCHAR(128) NOT NULL COMMENT '权益名称';
ALTER TABLE benefits MODIFY COLUMN status VARCHAR(32) NOT NULL COMMENT '权益发放状态';
ALTER TABLE benefits MODIFY COLUMN fail_reason VARCHAR(255) NULL COMMENT '权益发放失败原因';
ALTER TABLE benefits MODIFY COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';

ALTER TABLE agent_sessions COMMENT = 'Agent 问答会话记录表';
ALTER TABLE agent_sessions MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID';
ALTER TABLE agent_sessions MODIFY COLUMN trace_id VARCHAR(64) NOT NULL COMMENT '链路追踪ID';
ALTER TABLE agent_sessions MODIFY COLUMN question TEXT NOT NULL COMMENT '用户或客服提问内容';
ALTER TABLE agent_sessions MODIFY COLUMN answer TEXT NULL COMMENT 'Agent 回答内容';
ALTER TABLE agent_sessions MODIFY COLUMN duration_ms BIGINT NOT NULL DEFAULT 0 COMMENT '本次问答耗时毫秒数';
ALTER TABLE agent_sessions MODIFY COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';

ALTER TABLE agent_tool_logs COMMENT = 'Agent 工具调用日志表';
ALTER TABLE agent_tool_logs MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID';
ALTER TABLE agent_tool_logs MODIFY COLUMN trace_id VARCHAR(64) NOT NULL COMMENT '链路追踪ID';
ALTER TABLE agent_tool_logs MODIFY COLUMN tool_name VARCHAR(128) NOT NULL COMMENT '工具名称';
ALTER TABLE agent_tool_logs MODIFY COLUMN input_json TEXT NOT NULL COMMENT '工具入参 JSON';
ALTER TABLE agent_tool_logs MODIFY COLUMN output_summary TEXT NULL COMMENT '工具出参摘要';
ALTER TABLE agent_tool_logs MODIFY COLUMN duration_ms BIGINT NOT NULL COMMENT '工具调用耗时毫秒数';
ALTER TABLE agent_tool_logs MODIFY COLUMN success TINYINT(1) NOT NULL COMMENT '是否调用成功，1成功，0失败';
ALTER TABLE agent_tool_logs MODIFY COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';

ALTER TABLE rag_hit_logs COMMENT = 'RAG 知识命中日志表';
ALTER TABLE rag_hit_logs MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID';
ALTER TABLE rag_hit_logs MODIFY COLUMN trace_id VARCHAR(64) NOT NULL COMMENT '链路追踪ID';
ALTER TABLE rag_hit_logs MODIFY COLUMN source VARCHAR(255) NOT NULL COMMENT '命中的知识来源文件';
ALTER TABLE rag_hit_logs MODIFY COLUMN score DECIMAL(12, 8) NOT NULL COMMENT '向量检索相似度得分';
ALTER TABLE rag_hit_logs MODIFY COLUMN content_preview VARCHAR(512) NOT NULL COMMENT '命中文本摘要';
ALTER TABLE rag_hit_logs MODIFY COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';


-- ----------------------------------------------------------------------
-- Source: V4__knowledge_articles.sql
-- ----------------------------------------------------------------------
CREATE TABLE knowledge_articles (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    doc_code VARCHAR(64) NOT NULL UNIQUE,
    title VARCHAR(255) NOT NULL,
    category VARCHAR(64) NOT NULL,
    content TEXT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PUBLISHED',
    version INT NOT NULL DEFAULT 1,
    vector_synced_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_knowledge_articles_status (status),
    INDEX idx_knowledge_articles_category (category)
);

INSERT INTO knowledge_articles (doc_code, title, category, content, status, version) VALUES
('refund-policy', '退款政策', 'refund',
 'Orders can be refunded within 7 days after completion.\nCompleted orders older than 7 days are rejected unless customer-service manager approval is granted.\nRefunds are not allowed when the payment callback is missing because the order state must be repaired first.',
 'PUBLISHED', 1),
('payment-troubleshooting', '支付异常处理', 'payment',
 'When payment status is SUCCESS but callback_status is FAILED, the order may remain in PENDING_PAYMENT_CALLBACK.\nCustomer service should ask operations to trigger payment callback compensation.\nDo not ask the user to pay again before checking payment channel records.',
 'PUBLISHED', 1),
('member-benefits', '会员权益说明', 'benefit',
 'If benefit status is FAILED, check fail_reason first.\nFor benefit service timeout, customer service can retry benefit issuance after confirming payment success.\nSVIP users should receive monthly bonus benefits after a completed paid order.',
 'PUBLISHED', 1);


-- ----------------------------------------------------------------------
-- Source: V5__agent_production_features.sql
-- ----------------------------------------------------------------------
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


-- ----------------------------------------------------------------------
-- Source: V6__add_v4_v5_table_comments.sql
-- ----------------------------------------------------------------------
-- V4 知识库文档表
ALTER TABLE knowledge_articles COMMENT = '客服知识文档表（MySQL 真相源）';
ALTER TABLE knowledge_articles MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID';
ALTER TABLE knowledge_articles MODIFY COLUMN doc_code VARCHAR(64) NOT NULL COMMENT '文档唯一编码，如 refund-policy';
ALTER TABLE knowledge_articles MODIFY COLUMN title VARCHAR(255) NOT NULL COMMENT '文档标题';
ALTER TABLE knowledge_articles MODIFY COLUMN category VARCHAR(64) NOT NULL COMMENT '知识分类：refund/payment/benefit';
ALTER TABLE knowledge_articles MODIFY COLUMN content TEXT NOT NULL COMMENT '文档正文';
ALTER TABLE knowledge_articles MODIFY COLUMN status VARCHAR(32) NOT NULL DEFAULT 'PUBLISHED' COMMENT '文档状态：DRAFT/PUBLISHED';
ALTER TABLE knowledge_articles MODIFY COLUMN version INT NOT NULL DEFAULT 1 COMMENT '文档版本号，每次更新递增';
ALTER TABLE knowledge_articles MODIFY COLUMN vector_synced_at TIMESTAMP NULL COMMENT '最近一次同步到 Qdrant 的时间';
ALTER TABLE knowledge_articles MODIFY COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';
ALTER TABLE knowledge_articles MODIFY COLUMN updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';

-- V5 知识切片表
ALTER TABLE knowledge_chunks COMMENT = '知识文档切片表（同步 Qdrant 的最小检索单元）';
ALTER TABLE knowledge_chunks MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID';
ALTER TABLE knowledge_chunks MODIFY COLUMN doc_code VARCHAR(64) NOT NULL COMMENT '关联 knowledge_articles.doc_code';
ALTER TABLE knowledge_chunks MODIFY COLUMN chunk_index INT NOT NULL COMMENT '文档内切片序号，从 0 开始';
ALTER TABLE knowledge_chunks MODIFY COLUMN vector_point_id VARCHAR(64) NOT NULL COMMENT 'Qdrant point ID，与向量库一一对应';
ALTER TABLE knowledge_chunks MODIFY COLUMN content TEXT NOT NULL COMMENT '切片正文';
ALTER TABLE knowledge_chunks MODIFY COLUMN article_version INT NOT NULL COMMENT '切片所属文档版本号';
ALTER TABLE knowledge_chunks MODIFY COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';

-- V5 Agent 多轮会话表
ALTER TABLE agent_conversations COMMENT = 'Agent 多轮对话会话表';
ALTER TABLE agent_conversations MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID';
ALTER TABLE agent_conversations MODIFY COLUMN session_id VARCHAR(64) NOT NULL COMMENT '会话ID，客户端多轮对话透传';
ALTER TABLE agent_conversations MODIFY COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';
ALTER TABLE agent_conversations MODIFY COLUMN updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';

-- V5 Agent 对话轮次表
ALTER TABLE agent_conversation_turns COMMENT = 'Agent 多轮对话轮次表';
ALTER TABLE agent_conversation_turns MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID';
ALTER TABLE agent_conversation_turns MODIFY COLUMN session_id VARCHAR(64) NOT NULL COMMENT '所属会话ID';
ALTER TABLE agent_conversation_turns MODIFY COLUMN turn_index INT NOT NULL COMMENT '会话内轮次序号';
ALTER TABLE agent_conversation_turns MODIFY COLUMN trace_id VARCHAR(64) NOT NULL COMMENT '本轮问答链路追踪ID';
ALTER TABLE agent_conversation_turns MODIFY COLUMN role VARCHAR(16) NOT NULL COMMENT '发言角色：user/assistant';
ALTER TABLE agent_conversation_turns MODIFY COLUMN content TEXT NOT NULL COMMENT '发言内容';
ALTER TABLE agent_conversation_turns MODIFY COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';

-- V5 Agent 写操作待确认表（HITL）
ALTER TABLE agent_action_requests COMMENT = 'Agent 写操作待确认表（人机确认 HITL）';
ALTER TABLE agent_action_requests MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID';
ALTER TABLE agent_action_requests MODIFY COLUMN action_id VARCHAR(64) NOT NULL COMMENT '操作唯一ID，供确认/拒绝接口使用';
ALTER TABLE agent_action_requests MODIFY COLUMN session_id VARCHAR(64) NULL COMMENT '触发操作的会话ID';
ALTER TABLE agent_action_requests MODIFY COLUMN trace_id VARCHAR(64) NOT NULL COMMENT '触发操作的链路追踪ID';
ALTER TABLE agent_action_requests MODIFY COLUMN action_type VARCHAR(64) NOT NULL COMMENT '操作类型：SUBMIT_REFUND/RETRY_BENEFIT';
ALTER TABLE agent_action_requests MODIFY COLUMN order_no VARCHAR(64) NOT NULL COMMENT '关联业务订单号';
ALTER TABLE agent_action_requests MODIFY COLUMN payload_json TEXT NULL COMMENT '操作入参 JSON，如退款原因';
ALTER TABLE agent_action_requests MODIFY COLUMN status VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/CONFIRMED/REJECTED/EXECUTED';
ALTER TABLE agent_action_requests MODIFY COLUMN reason TEXT NULL COMMENT '拒绝原因或备注';
ALTER TABLE agent_action_requests MODIFY COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';
ALTER TABLE agent_action_requests MODIFY COLUMN updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';

-- V5 RAG 评测用例表
ALTER TABLE rag_eval_cases COMMENT = 'RAG 检索评测用例表';
ALTER TABLE rag_eval_cases MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID';
ALTER TABLE rag_eval_cases MODIFY COLUMN question TEXT NOT NULL COMMENT '评测问题';
ALTER TABLE rag_eval_cases MODIFY COLUMN expected_doc_code VARCHAR(64) NOT NULL COMMENT '期望命中的文档编码';
ALTER TABLE rag_eval_cases MODIFY COLUMN category VARCHAR(64) NULL COMMENT '可选分类过滤：refund/payment/benefit';
ALTER TABLE rag_eval_cases MODIFY COLUMN enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用，1启用，0禁用';
ALTER TABLE rag_eval_cases MODIFY COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';

-- V5 agent_sessions 新增字段
ALTER TABLE agent_sessions MODIFY COLUMN session_id VARCHAR(64) NULL COMMENT '多轮会话ID，关联 agent_conversations';
ALTER TABLE agent_sessions MODIFY COLUMN turn_index INT NOT NULL DEFAULT 0 COMMENT '会话内轮次序号';


-- ----------------------------------------------------------------------
-- Source: V7__fix_conversation_turn_unique_key.sql
-- ----------------------------------------------------------------------
-- 同一轮问答中 user/assistant 共用 turn_index，唯一键需包含 role
ALTER TABLE agent_conversation_turns
    DROP INDEX uk_agent_turns_session_index;

ALTER TABLE agent_conversation_turns
    ADD UNIQUE KEY uk_agent_turns_session_role (session_id, turn_index, role);


-- ----------------------------------------------------------------------
-- Source: V8__knowledge_articles_zh_content.sql
-- ----------------------------------------------------------------------
-- 将初始三篇知识库正文优化为中文（便于 RAG 检索与客服 Agent 引用）

UPDATE knowledge_articles SET
    content = '【适用范围】所有已完成或进行中的订单退款咨询。

【基本规则】
1. 订单状态为 COMPLETED（已完成）的，自完成时间起 7 个自然日内可申请退款。
2. 超过 7 天的已完成订单，系统默认拒绝退款（refundStatus=REJECTED），常见拒绝原因：「订单完成已超过 7 天」。
3. 超过 7 天仍要退款，须客服主管审批后方可特批，不可直接告知用户「可以退」。

【不可退款情形】
- 支付回调缺失（paymentCallbackStatus 为空或异常）且订单仍停留在待回调状态（如 PENDING_PAYMENT_CALLBACK）时，应先修复订单状态，不得直接发起退款。
- 订单未完成、未支付成功的，走取消/关单流程，不走退款流程。

【客服话术要点】
- 先查订单完成时间 completedAt 与当前退款状态 refundStatus、拒绝原因 refundReason。
- 在 7 天内：可引导用户提交退款申请（需人工确认后写入退款表）。
- 超过 7 天：说明政策限制，如需特批请升级主管。

【关联字段】orderStatus、completedAt、refundStatus、refundReason、paymentStatus',
    version = version + 1,
    vector_synced_at = NULL
WHERE doc_code = 'refund-policy';

UPDATE knowledge_articles SET
    content = '【适用范围】用户反馈「已支付但订单状态不对」「支付成功页面未跳转」等支付异常。

【典型现象】
- paymentStatus = SUCCESS（渠道侧扣款成功）
- paymentCallbackStatus = FAILED（支付回调失败）
- orderStatus 仍停留在 PENDING_PAYMENT_CALLBACK（待支付回调）

【处理原则】
1. 先查订单号对应的支付记录与回调状态，不要凭用户口述判断。
2. 确认为「支付成功 + 回调失败」时，由运营/技术触发支付回调补偿（模拟环境可在后台点「支付回调成功」）。
3. 在确认支付渠道侧已有成功记录前，禁止引导用户重复支付。
4. 回调补偿成功后，订单状态应流转至正常后续状态（如待发货/已完成等，视业务而定）。

【排查步骤】
1. 查 paymentStatus、paymentCallbackStatus、orderStatus、paidAt。
2. 查是否有统一下单/支付请求日志（是否真正发起过支付）。
3. paymentStatus = PENDING 且无支付记录 → 用户未完成支付，非通道异常。
4. paymentStatus = FAILED → 结合失败原因排查（余额不足、超时、用户取消等）。

【客服话术要点】
- 支付已成功且仅回调延迟/失败：安抚用户，说明正在核实，勿重复付款。
- 未发起支付：引导用户在订单页重新支付。

【关联字段】paymentStatus、paymentCallbackStatus、orderStatus、paidAt、channel',
    version = version + 1,
    vector_synced_at = NULL
WHERE doc_code = 'payment-troubleshooting';

UPDATE knowledge_articles SET
    content = '【适用范围】会员/权益未到账、权益发放失败类咨询。

【基本规则】
1. 用户支付成功且订单正常完成后，应按会员等级发放对应权益（如 SVIP 月度礼包「SVIP monthly bonus」）。
2. 权益发放状态以 benefits 表为准，常见状态：PENDING（发放中）、ISSUED（已发放）、FAILED（失败）。

【权益失败处理】
1. 若 benefitStatus = FAILED，必须先查看 fail_reason（失败原因）。
2. 失败原因为「benefit service timeout」（权益服务超时）且 paymentStatus = SUCCESS 时：
   - 可为用户创建权益重试申请（retry_benefit_issue），经人工确认后执行补发。
   - 不可在未确认支付成功的情况下重试。
3. 非超时类失败（如资格不符、库存不足），需按具体原因解释，不可盲目重试。

【客服话术要点】
- 先确认订单已支付成功、订单状态是否已完成或处于可发放权益的状态。
- 权益 PENDING：说明正在处理，请稍后查看。
- 权益 FAILED + 超时：核实支付后可申请补发，需人工确认。

【关联字段】benefits、benefitStatus、fail_reason、paymentStatus、orderStatus、userNo/会员等级',
    version = version + 1,
    vector_synced_at = NULL
WHERE doc_code = 'member-benefits';


-- ----------------------------------------------------------------------
-- Source: V9__agent_prompt_templates.sql
-- ----------------------------------------------------------------------
CREATE TABLE agent_prompt_templates (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    prompt_code VARCHAR(64) NOT NULL,
    title VARCHAR(128) NOT NULL,
    description VARCHAR(512) NULL,
    content TEXT NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    version INT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_agent_prompt_code (prompt_code)
) COMMENT='Agent 提示词模板（MySQL 维护，运行时加载）';


-- ----------------------------------------------------------------------
-- Source: V10__prompt_code_structure.sql
-- ----------------------------------------------------------------------
-- 提示词编码细化：业务领域 / 模块 / 场景 / Agent 模式 / 提示词角色
-- 编码格式: {bizDomain}.{bizModule}.{scene}.{agentMode}.{promptRole}
-- 示例: order.cs.chat.framework.system

ALTER TABLE agent_prompt_templates
    ADD COLUMN biz_domain VARCHAR(32) NOT NULL DEFAULT 'order' COMMENT '业务领域，如 order' AFTER prompt_code,
    ADD COLUMN biz_module VARCHAR(32) NOT NULL DEFAULT 'cs' COMMENT '业务模块，如 cs=客服' AFTER biz_domain,
    ADD COLUMN scene VARCHAR(32) NOT NULL DEFAULT 'chat' COMMENT '业务场景，如 chat=对话' AFTER biz_module,
    ADD COLUMN agent_mode VARCHAR(32) NOT NULL DEFAULT 'framework' COMMENT 'Agent 模式：framework/manual' AFTER scene,
    ADD COLUMN prompt_role VARCHAR(32) NOT NULL DEFAULT 'system' COMMENT '提示词角色：system/user/tool_retry' AFTER agent_mode;

UPDATE agent_prompt_templates
SET prompt_code = 'order.cs.chat.framework.system',
    biz_domain = 'order',
    biz_module = 'cs',
    scene = 'chat',
    agent_mode = 'framework',
    prompt_role = 'system'
WHERE prompt_code = 'framework_system';

UPDATE agent_prompt_templates
SET prompt_code = 'order.cs.chat.manual.system',
    biz_domain = 'order',
    biz_module = 'cs',
    scene = 'chat',
    agent_mode = 'manual',
    prompt_role = 'system'
WHERE prompt_code = 'manual_system';

UPDATE agent_prompt_templates
SET prompt_code = 'order.cs.chat.framework.user',
    biz_domain = 'order',
    biz_module = 'cs',
    scene = 'chat',
    agent_mode = 'framework',
    prompt_role = 'user'
WHERE prompt_code = 'framework_user';

UPDATE agent_prompt_templates
SET prompt_code = 'order.cs.chat.framework.tool_retry',
    biz_domain = 'order',
    biz_module = 'cs',
    scene = 'chat',
    agent_mode = 'framework',
    prompt_role = 'tool_retry'
WHERE prompt_code = 'framework_tool_retry_suffix';

UPDATE agent_prompt_templates
SET prompt_code = 'order.cs.chat.manual.user',
    biz_domain = 'order',
    biz_module = 'cs',
    scene = 'chat',
    agent_mode = 'manual',
    prompt_role = 'user'
WHERE prompt_code = 'manual_user';

CREATE INDEX idx_agent_prompt_domain_module ON agent_prompt_templates (biz_domain, biz_module, scene);


-- ----------------------------------------------------------------------
-- Source: V11__agent_conversation_session_state.sql
-- ----------------------------------------------------------------------
ALTER TABLE agent_conversations
    ADD COLUMN session_state JSON NULL COMMENT '会话状态 JSON' AFTER session_id;

