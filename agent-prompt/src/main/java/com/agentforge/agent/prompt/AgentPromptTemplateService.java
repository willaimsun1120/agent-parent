package com.agentforge.agent.prompt;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.agentforge.agent.cache.CachedPromptTemplateService;
import com.agentforge.agent.core.spi.PromptTemplateSeeder;
import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Agent 提示词模板业务服务。
 *
 * <p>负责启动时种子数据写入、模板 CRUD 及更新后 JetCache 刷新。
 */
@Service
public class AgentPromptTemplateService {
    private static final Logger log = LoggerFactory.getLogger(AgentPromptTemplateService.class);

    private final AgentPromptTemplateMapper mapper;
    private final CachedPromptTemplateService cachedPromptTemplateService;
    private final PromptTemplateSeeder seeder;

    /**
     * 构造模板业务服务。
     *
     * @param mapper                     模板 Mapper
     * @param cachedPromptTemplateService 缓存模板服务
     * @param seeder                     默认模板种子提供者
     */
    public AgentPromptTemplateService(AgentPromptTemplateMapper mapper,
                                      CachedPromptTemplateService cachedPromptTemplateService,
                                      PromptTemplateSeeder seeder) {
        this.mapper = mapper;
        this.cachedPromptTemplateService = cachedPromptTemplateService;
        this.seeder = seeder;
    }

    /** 启动时若表为空则写入 SPI 默认模板，并预热 JetCache。 */
    @PostConstruct
    void seedDefaultsIfEmpty() {
        Long count = mapper.selectCount(null);
        if (count != null && count > 0) {
            cachedPromptTemplateService.warmup();
            return;
        }
        // 将 SPI 提供的默认模板写入数据库
        for (Map.Entry<String, PromptTemplateSeeder.DefaultPrompt> entry : seeder.defaults().entrySet()) {
            PromptTemplateSeeder.DefaultPrompt def = entry.getValue();
            PromptCode.Parsed parsed = PromptCode.parse(entry.getKey());
            AgentPromptTemplate template = new AgentPromptTemplate();
            template.setPromptCode(parsed.promptCode());
            template.setBizDomain(parsed.bizDomain());
            template.setBizModule(parsed.bizModule());
            template.setScene(parsed.scene());
            template.setAgentMode(parsed.agentMode());
            template.setPromptRole(parsed.promptRole());
            template.setTitle(def.name());
            template.setDescription(def.description());
            template.setContent(def.content().trim());
            template.setStatus("ACTIVE");
            template.setVersion(1);
            mapper.insert(template);
        }
        log.info("已初始化 {} 条默认 Agent 提示词模板", seeder.defaults().size());
        cachedPromptTemplateService.warmup();
    }

    /**
     * 查询全部模板，按业务维度排序。
     *
     * @return 模板列表
     */
    public List<AgentPromptTemplate> listAll() {
        return mapper.selectList(new LambdaQueryWrapper<AgentPromptTemplate>()
            .orderByAsc(AgentPromptTemplate::getBizDomain)
            .orderByAsc(AgentPromptTemplate::getBizModule)
            .orderByAsc(AgentPromptTemplate::getScene)
            .orderByAsc(AgentPromptTemplate::getAgentMode)
            .orderByAsc(AgentPromptTemplate::getPromptRole));
    }

    /**
     * 按 promptCode 查询模板，不存在时抛出 404。
     *
     * @param promptCode 模板编码（支持旧版别名）
     * @return 模板实体
     */
    public AgentPromptTemplate getByCode(String promptCode) {
        String normalized = PromptCodeResolver.resolve(promptCode);
        AgentPromptTemplate template = mapper.selectOne(new LambdaQueryWrapper<AgentPromptTemplate>()
            .eq(AgentPromptTemplate::getPromptCode, normalized)
            .last("LIMIT 1"));
        if (template == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Prompt template not found: " + promptCode);
        }
        return template;
    }

    /**
     * 更新模板内容与元数据，版本号自增并刷新缓存。
     *
     * @param promptCode 模板编码
     * @param request    更新请求
     * @return 更新后的模板实体
     */
    public AgentPromptTemplate update(String promptCode, PromptTemplateUpdateRequest request) {
        String normalized = PromptCodeResolver.resolve(promptCode);
        AgentPromptTemplate existing = getByCode(normalized);
        if (request.content() == null || request.content().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "content 不能为空");
        }
        if (request.title() != null && !request.title().isBlank()) {
            existing.setTitle(request.title().trim());
        }
        if (request.description() != null) {
            existing.setDescription(request.description().trim());
        }
        existing.setContent(request.content().trim());
        if (request.status() != null && !request.status().isBlank()) {
            existing.setStatus(normalizeStatus(request.status()));
        }
        existing.setVersion(existing.getVersion() + 1);
        mapper.updateById(existing);
        cachedPromptTemplateService.refresh(normalized);
        log.info("更新提示词模板 promptCode={} version={}，JetCache 已刷新", normalized, existing.getVersion());
        return getByCode(normalized);
    }

    /** 校验并规范化模板状态为 ACTIVE 或 DRAFT。 */
    private String normalizeStatus(String status) {
        String normalized = status.trim().toUpperCase();
        if (!"ACTIVE".equals(normalized) && !"DRAFT".equals(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "status 仅支持 ACTIVE 或 DRAFT");
        }
        return normalized;
    }
}
