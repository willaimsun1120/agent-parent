-- ======================================================================
-- AgentForge - HR 客服 Demo 完整建库脚本
-- Database: hr_agent_demo
-- Generated from Flyway migrations: hr-cs-example
-- Migrations: V1__create_hr_agent_schema.sql, V2__insert_demo_data.sql, V3__knowledge_articles_zh_content.sql, V4__add_table_and_column_comments.sql, V5__fix_conversation_turn_unique_key.sql
-- ======================================================================

CREATE DATABASE IF NOT EXISTS `hr_agent_demo` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `hr_agent_demo`;

-- ----------------------------------------------------------------------
-- Source: V1__create_hr_agent_schema.sql
-- ----------------------------------------------------------------------
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


-- ----------------------------------------------------------------------
-- Source: V2__insert_demo_data.sql
-- ----------------------------------------------------------------------
INSERT INTO employees (emp_no, name, department, annual_leave_balance, hire_date) VALUES
('EMP-1001', '张三', '研发部', 10, '2022-03-15'),
('EMP-1002', '李四', '市场部', 2, '2023-08-01'),
('EMP-1003', '王五', '财务部', 0, '2020-01-10');

INSERT INTO leave_requests (emp_no, leave_type, days, status, reason, start_date) VALUES
('EMP-1001', 'ANNUAL', 3, 'APPROVED', '家庭事务', '2026-07-01'),
('EMP-1002', 'ANNUAL', 2, 'PENDING', '个人休息', '2026-06-20'),
('EMP-1003', 'ANNUAL', 5, 'REJECTED', '年假余额不足', '2026-06-15');

INSERT INTO payroll_records (emp_no, pay_month, amount, status, paid_at) VALUES
('EMP-1001', '2026-05', 18000.00, 'PAID', '2026-05-10 10:00:00'),
('EMP-1002', '2026-05', 12000.00, 'DELAYED', NULL),
('EMP-1003', '2026-05', 15000.00, 'PAID', '2026-05-10 10:00:00');

INSERT INTO hr_benefits (emp_no, benefit_type, status) VALUES
('EMP-1001', 'SOCIAL_INSURANCE', 'ACTIVE'),
('EMP-1001', 'SUPPLEMENTARY_MEDICAL', 'ACTIVE'),
('EMP-1002', 'SOCIAL_INSURANCE', 'ACTIVE'),
('EMP-1002', 'SUPPLEMENTARY_MEDICAL', 'PENDING'),
('EMP-1003', 'SOCIAL_INSURANCE', 'ACTIVE'),
('EMP-1003', 'HOUSING_FUND', 'INACTIVE');


-- ----------------------------------------------------------------------
-- Source: V3__knowledge_articles_zh_content.sql
-- ----------------------------------------------------------------------
INSERT INTO knowledge_articles (doc_code, title, category, content, status, version) VALUES
('leave-policy', '请假与年假政策', 'leave',
 '员工年假按入职年限计算，每年 1 月 1 日更新余额。\n年假余额不足时，年假类请假申请将被拒绝；可使用事假或调休替代。\n请假需提前 3 个工作日提交申请，3 天以内由直属主管审批，3 天以上需 HR 复核。\n已批准的请假可在系统中查询状态；待审批申请一般 1-2 个工作日内处理。',
 'PUBLISHED', 1),
('payroll-faq', '工资发放常见问题', 'payroll',
 '工资于每月 10 日前发放至员工绑定的银行卡。\n若状态显示 DELAYED（延迟发放），常见原因为：考勤异常待确认、个税信息变更、或银行账号有误。\n客服应先核对 payroll 记录中的 pay_month 与 status，再联系 HR 薪酬组核实。\n请勿重复提交发薪申请；确认账号无误后等待补发即可。',
 'PUBLISHED', 1),
('benefit-guide', '社保与福利指南', 'benefit',
 '入职后公司统一缴纳五险一金；补充医疗保险需在入职 30 天内完成 enrollment。\nbenefit_type 为 SUPPLEMENTARY_MEDICAL 时，PENDING 表示待员工在福利平台确认。\nINACTIVE 表示未开通或已停保；ACTIVE 表示正常生效。\n员工要求变更福利 enrollment 时，需创建待确认申请，由 HR 人工审核后生效。',
 'PUBLISHED', 1);


-- ----------------------------------------------------------------------
-- Source: V4__add_table_and_column_comments.sql
-- ----------------------------------------------------------------------
-- HR 业务表
ALTER TABLE employees COMMENT = '员工主表';
ALTER TABLE employees MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID';
ALTER TABLE employees MODIFY COLUMN emp_no VARCHAR(64) NOT NULL COMMENT '员工工号，如 EMP-1001';
ALTER TABLE employees MODIFY COLUMN name VARCHAR(128) NOT NULL COMMENT '员工姓名';
ALTER TABLE employees MODIFY COLUMN department VARCHAR(128) NOT NULL COMMENT '所属部门';
ALTER TABLE employees MODIFY COLUMN annual_leave_balance INT NOT NULL DEFAULT 0 COMMENT '剩余年假天数';
ALTER TABLE employees MODIFY COLUMN hire_date DATE NOT NULL COMMENT '入职日期';
ALTER TABLE employees MODIFY COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';

ALTER TABLE leave_requests COMMENT = '请假申请记录表';
ALTER TABLE leave_requests MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID';
ALTER TABLE leave_requests MODIFY COLUMN emp_no VARCHAR(64) NOT NULL COMMENT '员工工号';
ALTER TABLE leave_requests MODIFY COLUMN leave_type VARCHAR(32) NOT NULL COMMENT '请假类型：ANNUAL/SICK/PERSONAL 等';
ALTER TABLE leave_requests MODIFY COLUMN days INT NOT NULL COMMENT '请假天数';
ALTER TABLE leave_requests MODIFY COLUMN status VARCHAR(32) NOT NULL COMMENT '审批状态：PENDING/APPROVED/REJECTED';
ALTER TABLE leave_requests MODIFY COLUMN reason VARCHAR(255) NULL COMMENT '请假原因或拒绝原因';
ALTER TABLE leave_requests MODIFY COLUMN start_date DATE NULL COMMENT '请假开始日期';
ALTER TABLE leave_requests MODIFY COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';

ALTER TABLE payroll_records COMMENT = '工资发放记录表';
ALTER TABLE payroll_records MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID';
ALTER TABLE payroll_records MODIFY COLUMN emp_no VARCHAR(64) NOT NULL COMMENT '员工工号';
ALTER TABLE payroll_records MODIFY COLUMN pay_month VARCHAR(7) NOT NULL COMMENT '发薪月份，格式 yyyy-MM';
ALTER TABLE payroll_records MODIFY COLUMN amount DECIMAL(12, 2) NOT NULL COMMENT '应发工资金额';
ALTER TABLE payroll_records MODIFY COLUMN status VARCHAR(32) NOT NULL COMMENT '发放状态：PAID/DELAYED/PENDING';
ALTER TABLE payroll_records MODIFY COLUMN paid_at TIMESTAMP NULL COMMENT '实际发放时间';
ALTER TABLE payroll_records MODIFY COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';

ALTER TABLE hr_benefits COMMENT = '员工福利参保记录表';
ALTER TABLE hr_benefits MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID';
ALTER TABLE hr_benefits MODIFY COLUMN emp_no VARCHAR(64) NOT NULL COMMENT '员工工号';
ALTER TABLE hr_benefits MODIFY COLUMN benefit_type VARCHAR(64) NOT NULL COMMENT '福利类型：SOCIAL_INSURANCE/HOUSING_FUND/SUPPLEMENTARY_MEDICAL';
ALTER TABLE hr_benefits MODIFY COLUMN status VARCHAR(32) NOT NULL COMMENT '参保状态：ACTIVE/INACTIVE/PENDING';
ALTER TABLE hr_benefits MODIFY COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';

-- 平台可观测性
ALTER TABLE agent_sessions COMMENT = 'Agent 问答会话记录表';
ALTER TABLE agent_sessions MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID';
ALTER TABLE agent_sessions MODIFY COLUMN trace_id VARCHAR(64) NOT NULL COMMENT '链路追踪ID';
ALTER TABLE agent_sessions MODIFY COLUMN session_id VARCHAR(64) NULL COMMENT '多轮会话ID，关联 agent_conversations';
ALTER TABLE agent_sessions MODIFY COLUMN turn_index INT NOT NULL DEFAULT 0 COMMENT '会话内轮次序号';
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

-- 知识库
ALTER TABLE knowledge_articles COMMENT = 'HR 客服知识文档表（MySQL 真相源）';
ALTER TABLE knowledge_articles MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID';
ALTER TABLE knowledge_articles MODIFY COLUMN doc_code VARCHAR(64) NOT NULL COMMENT '文档唯一编码，如 leave-policy';
ALTER TABLE knowledge_articles MODIFY COLUMN title VARCHAR(255) NOT NULL COMMENT '文档标题';
ALTER TABLE knowledge_articles MODIFY COLUMN category VARCHAR(64) NOT NULL COMMENT '知识分类：leave/payroll/benefit';
ALTER TABLE knowledge_articles MODIFY COLUMN content TEXT NOT NULL COMMENT '文档正文';
ALTER TABLE knowledge_articles MODIFY COLUMN status VARCHAR(32) NOT NULL DEFAULT 'PUBLISHED' COMMENT '文档状态：DRAFT/PUBLISHED';
ALTER TABLE knowledge_articles MODIFY COLUMN version INT NOT NULL DEFAULT 1 COMMENT '文档版本号，每次更新递增';
ALTER TABLE knowledge_articles MODIFY COLUMN vector_synced_at TIMESTAMP NULL COMMENT '最近一次同步到 Qdrant 的时间';
ALTER TABLE knowledge_articles MODIFY COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';
ALTER TABLE knowledge_articles MODIFY COLUMN updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';

ALTER TABLE knowledge_chunks COMMENT = '知识文档切片表（同步 Qdrant 的最小检索单元）';
ALTER TABLE knowledge_chunks MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID';
ALTER TABLE knowledge_chunks MODIFY COLUMN doc_code VARCHAR(64) NOT NULL COMMENT '关联 knowledge_articles.doc_code';
ALTER TABLE knowledge_chunks MODIFY COLUMN chunk_index INT NOT NULL COMMENT '文档内切片序号，从 0 开始';
ALTER TABLE knowledge_chunks MODIFY COLUMN vector_point_id VARCHAR(64) NOT NULL COMMENT 'Qdrant point ID，与向量库一一对应';
ALTER TABLE knowledge_chunks MODIFY COLUMN content TEXT NOT NULL COMMENT '切片正文';
ALTER TABLE knowledge_chunks MODIFY COLUMN article_version INT NOT NULL COMMENT '切片所属文档版本号';
ALTER TABLE knowledge_chunks MODIFY COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';

-- 多轮对话与 HITL
ALTER TABLE agent_conversations COMMENT = 'Agent 多轮对话会话表';
ALTER TABLE agent_conversations MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID';
ALTER TABLE agent_conversations MODIFY COLUMN session_id VARCHAR(64) NOT NULL COMMENT '会话ID，客户端多轮对话透传';
ALTER TABLE agent_conversations MODIFY COLUMN session_state JSON NULL COMMENT '会话状态 JSON';
ALTER TABLE agent_conversations MODIFY COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';
ALTER TABLE agent_conversations MODIFY COLUMN updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';

ALTER TABLE agent_conversation_turns COMMENT = 'Agent 多轮对话轮次表';
ALTER TABLE agent_conversation_turns MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID';
ALTER TABLE agent_conversation_turns MODIFY COLUMN session_id VARCHAR(64) NOT NULL COMMENT '所属会话ID';
ALTER TABLE agent_conversation_turns MODIFY COLUMN turn_index INT NOT NULL COMMENT '会话内轮次序号';
ALTER TABLE agent_conversation_turns MODIFY COLUMN trace_id VARCHAR(64) NOT NULL COMMENT '本轮问答链路追踪ID';
ALTER TABLE agent_conversation_turns MODIFY COLUMN role VARCHAR(16) NOT NULL COMMENT '发言角色：user/assistant';
ALTER TABLE agent_conversation_turns MODIFY COLUMN content TEXT NOT NULL COMMENT '发言内容';
ALTER TABLE agent_conversation_turns MODIFY COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';

ALTER TABLE agent_action_requests COMMENT = 'Agent 写操作待确认表（人机确认 HITL）';
ALTER TABLE agent_action_requests MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID';
ALTER TABLE agent_action_requests MODIFY COLUMN action_id VARCHAR(64) NOT NULL COMMENT '操作唯一ID，供确认/拒绝接口使用';
ALTER TABLE agent_action_requests MODIFY COLUMN session_id VARCHAR(64) NULL COMMENT '触发操作的会话ID';
ALTER TABLE agent_action_requests MODIFY COLUMN trace_id VARCHAR(64) NOT NULL COMMENT '触发操作的链路追踪ID';
ALTER TABLE agent_action_requests MODIFY COLUMN action_type VARCHAR(64) NOT NULL COMMENT '操作类型：SUBMIT_LEAVE/UPDATE_BENEFIT';
ALTER TABLE agent_action_requests MODIFY COLUMN order_no VARCHAR(64) NOT NULL COMMENT '关联业务主键，HR Demo 中存储 emp_no';
ALTER TABLE agent_action_requests MODIFY COLUMN payload_json TEXT NULL COMMENT '操作入参 JSON，如请假天数、原因';
ALTER TABLE agent_action_requests MODIFY COLUMN status VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/CONFIRMED/REJECTED/EXECUTED';
ALTER TABLE agent_action_requests MODIFY COLUMN reason TEXT NULL COMMENT '拒绝原因或备注';
ALTER TABLE agent_action_requests MODIFY COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';
ALTER TABLE agent_action_requests MODIFY COLUMN updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';

ALTER TABLE rag_eval_cases COMMENT = 'RAG 检索评测用例表';
ALTER TABLE rag_eval_cases MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID';
ALTER TABLE rag_eval_cases MODIFY COLUMN question TEXT NOT NULL COMMENT '评测问题';
ALTER TABLE rag_eval_cases MODIFY COLUMN expected_doc_code VARCHAR(64) NOT NULL COMMENT '期望命中的文档编码';
ALTER TABLE rag_eval_cases MODIFY COLUMN category VARCHAR(64) NULL COMMENT '可选分类过滤：leave/payroll/benefit';
ALTER TABLE rag_eval_cases MODIFY COLUMN enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用，1启用，0禁用';
ALTER TABLE rag_eval_cases MODIFY COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';

-- 提示词模板
ALTER TABLE agent_prompt_templates COMMENT = 'Agent 提示词模板（MySQL 维护，运行时加载）';
ALTER TABLE agent_prompt_templates MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID';
ALTER TABLE agent_prompt_templates MODIFY COLUMN prompt_code VARCHAR(64) NOT NULL COMMENT '五段式提示词编码，如 hr.cs.chat.framework.system';
ALTER TABLE agent_prompt_templates MODIFY COLUMN biz_domain VARCHAR(32) NOT NULL DEFAULT 'hr' COMMENT '业务领域，如 hr';
ALTER TABLE agent_prompt_templates MODIFY COLUMN biz_module VARCHAR(32) NOT NULL DEFAULT 'cs' COMMENT '业务模块，如 cs=客服';
ALTER TABLE agent_prompt_templates MODIFY COLUMN scene VARCHAR(32) NOT NULL DEFAULT 'chat' COMMENT '业务场景，如 chat=对话';
ALTER TABLE agent_prompt_templates MODIFY COLUMN agent_mode VARCHAR(32) NOT NULL DEFAULT 'framework' COMMENT 'Agent 模式：framework/manual';
ALTER TABLE agent_prompt_templates MODIFY COLUMN prompt_role VARCHAR(32) NOT NULL DEFAULT 'system' COMMENT '提示词角色：system/user/tool_retry';
ALTER TABLE agent_prompt_templates MODIFY COLUMN title VARCHAR(128) NOT NULL COMMENT '模板标题';
ALTER TABLE agent_prompt_templates MODIFY COLUMN description VARCHAR(512) NULL COMMENT '模板说明';
ALTER TABLE agent_prompt_templates MODIFY COLUMN content TEXT NOT NULL COMMENT '模板正文';
ALTER TABLE agent_prompt_templates MODIFY COLUMN status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE/INACTIVE';
ALTER TABLE agent_prompt_templates MODIFY COLUMN version INT NOT NULL DEFAULT 1 COMMENT '模板版本号';
ALTER TABLE agent_prompt_templates MODIFY COLUMN created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';
ALTER TABLE agent_prompt_templates MODIFY COLUMN updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';


-- ----------------------------------------------------------------------
-- Source: V5__fix_conversation_turn_unique_key.sql
-- ----------------------------------------------------------------------
-- 每轮对话含 user + assistant 两条记录，唯一键须包含 role
ALTER TABLE agent_conversation_turns
    DROP INDEX uk_agent_turns_session_index;

ALTER TABLE agent_conversation_turns
    ADD UNIQUE KEY uk_agent_turns_session_role (session_id, turn_index, role);

