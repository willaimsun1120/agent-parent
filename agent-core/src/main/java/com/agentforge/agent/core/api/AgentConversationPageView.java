package com.agentforge.agent.core.api;

import java.util.List;

/**
 * 会话消息分页结果（聊天区向上无限滚动）。
 *
 * @param items         本页消息（时间正序：旧 → 新）
 * @param hasMore       是否还有更早消息
 * @param nextBeforeId  下一页游标（传给 beforeId）；没有更多时为 null
 */
public record AgentConversationPageView(
    List<AgentConversationMessageView> items,
    boolean hasMore,
    Long nextBeforeId
) {
}
