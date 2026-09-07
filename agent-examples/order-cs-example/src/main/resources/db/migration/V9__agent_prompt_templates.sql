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
