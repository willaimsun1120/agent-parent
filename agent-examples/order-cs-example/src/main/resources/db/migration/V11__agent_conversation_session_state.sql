ALTER TABLE agent_conversations
    ADD COLUMN session_state JSON NULL COMMENT '会话状态 JSON' AFTER session_id;
