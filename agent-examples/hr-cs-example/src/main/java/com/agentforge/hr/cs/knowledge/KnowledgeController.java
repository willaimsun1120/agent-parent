package com.agentforge.hr.cs.knowledge;

import com.agentforge.agent.vector.core.VectorStore;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 知识库 REST 控制器。
 *
 * <p>暴露文章 CRUD、发布与全量向量同步 API，供管理页面维护客服规则；
 * 同步结果写入当前向量库（Qdrant / Milvus），供 Agent RAG 检索使用。
 */
@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeController {
    private static final Logger log = LoggerFactory.getLogger(KnowledgeController.class);

    private final KnowledgeArticleService articleService;
    private final KnowledgeIngestionService ingestionService;
    private final VectorStore vectorStore;

    public KnowledgeController(KnowledgeArticleService articleService,
                               KnowledgeIngestionService ingestionService,
                               VectorStore vectorStore) {
        this.articleService = articleService;
        this.ingestionService = ingestionService;
        this.vectorStore = vectorStore;
    }

    /** 当前向量库配置（管理页展示 Qdrant / Milvus）。 */
    @GetMapping("/vector-store")
    public Map<String, String> vectorStoreInfo() {
        return Map.of(
            "type", vectorStore.type(),
            "collection", vectorStore.collectionName()
        );
    }

    /**
     * 查询全部知识文档列表。
     *
     * @return 文章列表
     */
    @GetMapping("/articles")
    public List<KnowledgeArticle> listArticles() {
        log.info("查询知识文档列表");
        return articleService.listArticles();
    }

    /**
     * 按 docCode 查询单篇文档。
     *
     * @param docCode 文档编码
     * @return 文章实体
     */
    @GetMapping("/articles/{docCode}")
    public KnowledgeArticle getArticle(@PathVariable String docCode) {
        log.info("查询知识文档 docCode={}", docCode);
        return articleService.getByDocCode(docCode);
    }

    /**
     * 创建新文档。
     *
     * @param request 创建请求
     * @return 创建后的文章
     */
    @PostMapping("/articles")
    public KnowledgeArticle createArticle(@RequestBody KnowledgeArticleRequest request) {
        log.info("创建知识文档 docCode={}", request == null ? null : request.docCode());
        return articleService.create(request);
    }

    /**
     * 更新已有文档。
     *
     * @param docCode 文档编码
     * @param request 更新请求
     * @return 更新后的文章
     */
    @PutMapping("/articles/{docCode}")
    public KnowledgeArticle updateArticle(@PathVariable String docCode,
                                          @RequestBody KnowledgeArticleRequest request) {
        log.info("更新知识文档 docCode={}", docCode);
        return articleService.update(docCode, request);
    }

    /**
     * 发布文档并同步向量至当前向量库。
     *
     * @param docCode 文档编码
     * @return 发布后的文章
     */
    @PostMapping("/articles/{docCode}/publish")
    public KnowledgeArticle publishArticle(@PathVariable String docCode) {
        log.info("发布知识文档 docCode={}", docCode);
        return articleService.publish(docCode);
    }

    /**
     * 全量同步全部已发布文档至当前向量库。
     *
     * @return 同步统计信息
     */
    @PostMapping("/sync")
    public Map<String, Object> syncAll() {
        log.info("同步全部已发布知识到 {} collection={}", vectorStore.type(), vectorStore.collectionName());
        return ingestionService.syncPublishedArticles();
    }
}
