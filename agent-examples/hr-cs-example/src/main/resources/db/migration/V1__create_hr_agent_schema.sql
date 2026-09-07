-- HR 业务表
CREATE TABLE employees (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    emp_no VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(128) NOT NULL,
    department VARCHAR(128) NOT NULL,
    annual_leave_balance INT NOT NULL DEFAULT 0,
    hire_date DATE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_employees_department (department)
);

CREATE TABLE leave_requests (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    emp_no VARCHAR(64) NOT NULL,
    leave_type VARCHAR(32) NOT NULL,
    days INT NOT NULL,
    status VARCHAR(32) NOT NULL,
    reason VARCHAR(255) NULL,
    start_date DATE NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_leave_requests_emp_no (emp_no)
);

CREATE TABLE payroll_records (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    emp_no VARCHAR(64) NOT NULL,
    pay_month VARCHAR(7) NOT NULL,
    amount DECIMAL(12, 2) NOT NULL,
    status VARCHAR(32) NOT NULL,
    paid_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_payroll_records_emp_no (emp_no),
    INDEX idx_payroll_records_pay_month (pay_month)
);

CREATE TABLE hr_benefits (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    emp_no VARCHAR(64) NOT NULL,
    benefit_type VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_hr_benefits_emp_no (emp_no)
);

-- 平台可观测性
CREATE TABLE agent_sessions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    trace_id VARCHAR(64) NOT NULL,
    session_id VARCHAR(64) NULL,
    turn_index INT NOT NULL DEFAULT 0,
    question TEXT NOT NULL,
    answer TEXT NULL,
    duration_ms BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_agent_sessions_trace_id (trace_id),
    INDEX idx_agent_sessions_session_id (session_id)
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

-- 知识库
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

-- 多轮对话与 HITL
CREATE TABLE agent_conversations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id VARCHAR(64) NOT NULL UNIQUE,
    session_state JSON NULL COMMENT '会话状态 JSON',
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
    order_no VARCHAR(64) NOT NULL COMMENT 'HR Demo 中存储 emp_no',
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

-- 提示词模板
CREATE TABLE agent_prompt_templates (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    prompt_code VARCHAR(64) NOT NULL,
    biz_domain VARCHAR(32) NOT NULL DEFAULT 'hr' COMMENT '业务领域，如 hr',
    biz_module VARCHAR(32) NOT NULL DEFAULT 'cs' COMMENT '业务模块，如 cs=客服',
    scene VARCHAR(32) NOT NULL DEFAULT 'chat' COMMENT '业务场景，如 chat=对话',
    agent_mode VARCHAR(32) NOT NULL DEFAULT 'framework' COMMENT 'Agent 模式：framework/manual',
    prompt_role VARCHAR(32) NOT NULL DEFAULT 'system' COMMENT '提示词角色：system/user/tool_retry',
    title VARCHAR(128) NOT NULL,
    description VARCHAR(512) NULL,
    content TEXT NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    version INT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_agent_prompt_code (prompt_code),
    INDEX idx_agent_prompt_domain_module (biz_domain, biz_module, scene)
) COMMENT='Agent 提示词模板（MySQL 维护，运行时加载）';

INSERT INTO rag_eval_cases (question, expected_doc_code, category) VALUES
('员工年假怎么申请？', 'leave-policy', 'leave'),
('超过年假余额还能请假吗？', 'leave-policy', 'leave'),
('工资还没到账怎么办？', 'payroll-faq', 'payroll'),
('本月工资为什么延迟发放？', 'payroll-faq', 'payroll'),
('社保福利怎么开通？', 'benefit-guide', 'benefit'),
('补充医疗保险如何变更？', 'benefit-guide', 'benefit');
