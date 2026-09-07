package com.agentforge.agent.rag;

import com.agentforge.agent.core.spi.KnowledgeIngestionFacade;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * RAG 知识库 HTTP 接口。
 *
 * <p>提供知识入库、向量检索与批量评测三类能力。
 */
@RestController
@RequestMapping("/api/rag")
public class RagController {
    private static final Logger log = LoggerFactory.getLogger(RagController.class);

    private final KnowledgeIngestionFacade ingestionFacade;
    private final RagSearchService ragSearchService;
    private final RagEvalService evalService;

    /**
     * 构造 RAG 控制器。
     *
     * @param ingestionFacade 知识入库门面
     * @param ragSearchService 检索服务
     * @param evalService      评测服务
     */
    public RagController(KnowledgeIngestionFacade ingestionFacade,
                         RagSearchService ragSearchService,
                         RagEvalService evalService) {
        this.ingestionFacade = ingestionFacade;
        this.ragSearchService = ragSearchService;
        this.evalService = evalService;
    }

    /**
     * 触发知识库全量入库（向量化并写入 Qdrant）。
     *
     * @return 入库结果摘要
     */
    @PostMapping("/ingest")
    public Map<String, Object> ingest() {
        long started = System.currentTimeMillis();
        log.info("收到知识库入库请求");
        Map<String, Object> result = ingestionFacade.ingestKnowledgeBase();
        log.info("知识库入库请求完成 result={} durationMs={}", result, System.currentTimeMillis() - started);
        return result;
    }

    /**
     * 按问题检索知识库。
     *
     * @param question 检索问题
     * @param topK     返回条数，默认 3
     * @param category 可选业务分类过滤
     * @return 命中知识片段列表
     */
    @GetMapping("/search")
    public List<KnowledgeDocument> search(@RequestParam String question,
                                            @RequestParam(defaultValue = "3") int topK,
                                            @RequestParam(required = false) String category) {
        long started = System.currentTimeMillis();
        log.info("收到 RAG 检索请求 question={} topK={} category={}", question, topK, category);
        List<KnowledgeDocument> hits = category == null || category.isBlank()
            ? ragSearchService.search(question, topK)
            : ragSearchService.search(question, topK, java.util.Optional.of(category));
        log.info("RAG 检索请求完成 hits={} durationMs={}", hits.size(), System.currentTimeMillis() - started);
        return hits;
    }

    /**
     * 执行 RAG 检索质量评测。
     *
     * @return 评测统计与逐条结果
     */
    @PostMapping("/eval")
    public Map<String, Object> evaluate() {
        log.info("收到 RAG 评测请求");
        return evalService.runEvaluation();
    }
}
