package com.agentforge.hr.cs.knowledge;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * 知识库文章业务服务。
 *
 * <p>管理文章的 CRUD 与发布流程；发布时触发 {@link KnowledgeIngestionService#syncArticle(String)}
 * 将内容切片并同步至 Qdrant，与平台 RAG 检索链路衔接。
 */
@Service
public class KnowledgeArticleService {
    private static final Logger log = LoggerFactory.getLogger(KnowledgeArticleService.class);

    private final KnowledgeArticleMapper articleMapper;
    private final KnowledgeIngestionService ingestionService;

    /**
     * 构造文章服务。
     *
     * @param articleMapper     文章 Mapper
     * @param ingestionService  向量同步服务
     */
    public KnowledgeArticleService(KnowledgeArticleMapper articleMapper,
                                   KnowledgeIngestionService ingestionService) {
        this.articleMapper = articleMapper;
        this.ingestionService = ingestionService;
    }

    /**
     * 查询全部文章，按更新时间倒序。
     *
     * @return 文章列表
     */
    public List<KnowledgeArticle> listArticles() {
        return articleMapper.selectList(new LambdaQueryWrapper<KnowledgeArticle>()
            .orderByDesc(KnowledgeArticle::getUpdatedAt));
    }

    /**
     * 查询已发布文章，按 docCode 升序。
     *
     * @return 已发布文章列表
     */
    public List<KnowledgeArticle> listPublishedArticles() {
        return articleMapper.selectList(new LambdaQueryWrapper<KnowledgeArticle>()
            .eq(KnowledgeArticle::getStatus, "PUBLISHED")
            .orderByAsc(KnowledgeArticle::getDocCode));
    }

    /**
     * 按 docCode 查询文章。
     *
     * @param docCode 文档编码
     * @return 文章实体
     * @throws ResponseStatusException 不存在时返回 404
     */
    public KnowledgeArticle getByDocCode(String docCode) {
        KnowledgeArticle article = articleMapper.selectOne(new LambdaQueryWrapper<KnowledgeArticle>()
            .eq(KnowledgeArticle::getDocCode, docCode)
            .last("LIMIT 1"));
        if (article == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Knowledge article not found: " + docCode);
        }
        return article;
    }

    /**
     * 创建新文章（默认 DRAFT 状态，版本 1）。
     *
     * @param request 创建请求
     * @return 创建后的文章
     * @throws ResponseStatusException docCode 冲突时返回 409
     */
    public KnowledgeArticle create(KnowledgeArticleRequest request) {
        validateRequest(request, true);
        if (existsDocCode(request.docCode())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "docCode already exists: " + request.docCode());
        }
        KnowledgeArticle article = toEntity(request, null);
        article.setStatus(normalizeStatus(request.status(), "DRAFT"));
        article.setVersion(1);
        articleMapper.insert(article);
        log.info("创建知识文档 docCode={} title={}", article.getDocCode(), article.getTitle());
        return getByDocCode(article.getDocCode());
    }

    /**
     * 更新已有文章（版本号递增，不自动同步向量）。
     *
     * @param docCode 文档编码
     * @param request 更新请求
     * @return 更新后的文章
     */
    public KnowledgeArticle update(String docCode, KnowledgeArticleRequest request) {
        KnowledgeArticle existing = getByDocCode(docCode);
        validateRequest(request, false);
        existing.setTitle(request.title().trim());
        existing.setCategory(request.category().trim());
        existing.setContent(request.content().trim());
        if (request.status() != null && !request.status().isBlank()) {
            existing.setStatus(normalizeStatus(request.status(), existing.getStatus()));
        }
        existing.setVersion(existing.getVersion() + 1);
        articleMapper.updateById(existing);
        log.info("更新知识文档 docCode={} version={}", docCode, existing.getVersion());
        return getByDocCode(docCode);
    }

    /**
     * 发布文章并同步向量至 Qdrant。
     *
     * @param docCode 文档编码
     * @return 发布并同步后的文章
     */
    public KnowledgeArticle publish(String docCode) {
        KnowledgeArticle article = getByDocCode(docCode);
        article.setStatus("PUBLISHED");
        article.setVersion(article.getVersion() + 1);
        articleMapper.updateById(article);
        ingestionService.syncArticle(docCode);
        article = getByDocCode(docCode);
        log.info("发布并同步知识文档 docCode={} vectorSyncedAt={}", docCode, article.getVectorSyncedAt());
        return article;
    }

    /** 检查 docCode 是否已存在。 */
    private boolean existsDocCode(String docCode) {
        return articleMapper.selectCount(new LambdaQueryWrapper<KnowledgeArticle>()
            .eq(KnowledgeArticle::getDocCode, docCode)) > 0;
    }

    /** 校验请求必填字段。 */
    private void validateRequest(KnowledgeArticleRequest request, boolean creating) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is required");
        }
        if (creating && (request.docCode() == null || request.docCode().isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "docCode is required");
        }
        if (request.title() == null || request.title().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "title is required");
        }
        if (request.category() == null || request.category().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "category is required");
        }
        if (request.content() == null || request.content().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "content is required");
        }
    }

    /** 将请求 DTO 转换为实体。 */
    private KnowledgeArticle toEntity(KnowledgeArticleRequest request, KnowledgeArticle existing) {
        KnowledgeArticle article = existing == null ? new KnowledgeArticle() : existing;
        if (existing == null) {
            article.setDocCode(request.docCode().trim());
        }
        article.setTitle(request.title().trim());
        article.setCategory(request.category().trim());
        article.setContent(request.content().trim());
        return article;
    }

    /** 规范化状态字符串为大写。 */
    private String normalizeStatus(String status, String defaultStatus) {
        if (status == null || status.isBlank()) {
            return defaultStatus;
        }
        return status.trim().toUpperCase();
    }
}
