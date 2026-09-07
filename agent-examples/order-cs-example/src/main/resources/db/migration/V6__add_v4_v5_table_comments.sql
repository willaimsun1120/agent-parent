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
