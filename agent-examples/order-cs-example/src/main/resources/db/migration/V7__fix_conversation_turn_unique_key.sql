-- 同一轮问答中 user/assistant 共用 turn_index，唯一键需包含 role
ALTER TABLE agent_conversation_turns
    DROP INDEX uk_agent_turns_session_index;

ALTER TABLE agent_conversation_turns
    ADD UNIQUE KEY uk_agent_turns_session_role (session_id, turn_index, role);
