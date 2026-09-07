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
