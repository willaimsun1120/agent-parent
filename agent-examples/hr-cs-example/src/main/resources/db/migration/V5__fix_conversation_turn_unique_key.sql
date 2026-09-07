-- 每轮对话含 user + assistant 两条记录，唯一键须包含 role
ALTER TABLE agent_conversation_turns
    DROP INDEX uk_agent_turns_session_index;

ALTER TABLE agent_conversation_turns
    ADD UNIQUE KEY uk_agent_turns_session_role (session_id, turn_index, role);
