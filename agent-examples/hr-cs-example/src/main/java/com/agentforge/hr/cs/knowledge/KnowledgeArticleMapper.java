package com.agentforge.hr.cs.knowledge;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 知识库文章表 MyBatis-Plus Mapper。
 *
 * <p>提供 {@link KnowledgeArticle} 的 CRUD 能力，供文章管理与向量同步流程使用。
 */
@Mapper
public interface KnowledgeArticleMapper extends BaseMapper<KnowledgeArticle> {
}
