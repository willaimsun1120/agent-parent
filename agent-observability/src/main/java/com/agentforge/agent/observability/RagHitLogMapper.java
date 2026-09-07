package com.agentforge.agent.observability;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * {@link RagHitLog} 的 MyBatis-Plus Mapper。
 *
 * <p>由 RAG 检索层写入命中记录，{@link ObservabilityController} 按 traceId 查询。
 */
public interface RagHitLogMapper extends BaseMapper<RagHitLog> {
}
