package com.agentforge.agent.api;

import com.agentforge.agent.prompt.AgentPromptTemplate;
import com.agentforge.agent.prompt.AgentPromptTemplateService;
import com.agentforge.agent.prompt.PromptTemplateUpdateRequest;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 提示词模板管理 HTTP 接口。
 *
 * <p>支持列表查询、按 promptCode 查询与更新模板内容。
 */
@RestController
@RequestMapping("/api/prompts")
public class PromptTemplateController {
    private static final Logger log = LoggerFactory.getLogger(PromptTemplateController.class);

    private final AgentPromptTemplateService service;

    /**
     * 构造提示词模板控制器。
     *
     * @param service 模板业务服务
     */
    public PromptTemplateController(AgentPromptTemplateService service) {
        this.service = service;
    }

    /**
     * 查询全部提示词模板列表。
     *
     * @return 按业务维度排序的模板列表
     */
    @GetMapping
    public List<AgentPromptTemplate> listPrompts() {
        log.info("查询提示词模板列表");
        return service.listAll();
    }

    /**
     * 按 promptCode 查询单条模板。
     *
     * @param promptCode 五段式模板编码（支持旧版别名）
     * @return 模板实体
     */
    @GetMapping("/{promptCode:.+}")
    public AgentPromptTemplate getPrompt(@PathVariable String promptCode) {
        log.info("查询提示词模板 promptCode={}", promptCode);
        return service.getByCode(promptCode);
    }

    /**
     * 更新指定 promptCode 的模板内容。
     *
     * @param promptCode 模板编码
     * @param request    更新请求体
     * @return 更新后的模板实体
     */
    @PutMapping("/{promptCode:.+}")
    public AgentPromptTemplate updatePrompt(@PathVariable String promptCode,
                                            @RequestBody PromptTemplateUpdateRequest request) {
        log.info("更新提示词模板 promptCode={}", promptCode);
        return service.update(promptCode, request);
    }
}
