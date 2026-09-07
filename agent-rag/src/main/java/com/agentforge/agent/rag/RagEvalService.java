package com.agentforge.agent.rag;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * RAG 检索质量评测服务。
 *
 * <p>从 {@code rag_eval_cases} 表加载启用用例，对每条问题执行检索并校验
 * top 结果是否包含期望的文档编码，汇总通过率。
 */
@Service
public class RagEvalService {

    private final RagEvalCaseMapper evalCaseMapper;
    private final RagSearchService ragSearchService;

    /**
     * 构造评测服务。
     *
     * @param evalCaseMapper    评测用例 Mapper
     * @param ragSearchService  RAG 检索门面
     */
    public RagEvalService(RagEvalCaseMapper evalCaseMapper, RagSearchService ragSearchService) {
        this.evalCaseMapper = evalCaseMapper;
        this.ragSearchService = ragSearchService;
    }

    /**
     * 执行全量评测并返回统计结果。
     *
     * @return 包含 total、passed、passRate、results 的汇总 Map
     */
    public Map<String, Object> runEvaluation() {
        List<RagEvalCase> cases = evalCaseMapper.selectList(new LambdaQueryWrapper<RagEvalCase>()
            .eq(RagEvalCase::getEnabled, true)
            .orderByAsc(RagEvalCase::getId));
        List<Map<String, Object>> results = new ArrayList<>();
        int passed = 0;
        // 逐条执行检索，判断 top 结果是否包含期望文档编码
        for (RagEvalCase evalCase : cases) {
            Optional<String> category = Optional.ofNullable(evalCase.getCategory()).filter(value -> !value.isBlank());
            List<KnowledgeDocument> hits = ragSearchService.search(evalCase.getQuestion(), 3, category);
            boolean hit = hits.stream().anyMatch(doc -> evalCase.getExpectedDocCode().equals(doc.docCode()));
            if (hit) {
                passed++;
            }
            results.add(Map.of(
                "question", evalCase.getQuestion(),
                "expectedDocCode", evalCase.getExpectedDocCode(),
                "passed", hit,
                "topHit", hits.isEmpty() ? "" : hits.get(0).citation()
            ));
        }
        return Map.of(
            "total", cases.size(),
            "passed", passed,
            "passRate", cases.isEmpty() ? 0 : (double) passed / cases.size(),
            "results", results
        );
    }
}
