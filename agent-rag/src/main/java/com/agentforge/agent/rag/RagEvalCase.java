package com.agentforge.agent.rag;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * RAG 检索评测用例实体，映射表 {@code rag_eval_cases}。
 */
@TableName("rag_eval_cases")
public class RagEvalCase {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String question;
    private String expectedDocCode;
    private String category;
    private Boolean enabled;
    private LocalDateTime createdAt;

    /** @return 主键 ID */
    public Long getId() { return id; }
    /** @return 评测问题 */
    public String getQuestion() { return question; }
    /** @return 期望命中的文档编码 */
    public String getExpectedDocCode() { return expectedDocCode; }
    /** @return 可选业务分类 */
    public String getCategory() { return category; }
    /** @return 是否启用 */
    public Boolean getEnabled() { return enabled; }
    /** @return 创建时间 */
    public LocalDateTime getCreatedAt() { return createdAt; }
}
