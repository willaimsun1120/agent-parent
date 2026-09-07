package com.agentforge.agent.prompt;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.agentforge.agent.core.spi.PromptTemplateSeeder;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 提示词模板数据库加载器。
 *
 * <p>从 {@code agent_prompt_templates} 表读取 ACTIVE 模板，缺失时回退 SPI 默认模板。
 */
@Component
public class PromptTemplateLoader {

    private final AgentPromptTemplateMapper mapper;
    private final PromptTemplateSeeder seeder;

    /**
     * 构造模板加载器。
     *
     * @param mapper  模板 Mapper
     * @param seeder  默认模板种子提供者
     */
    public PromptTemplateLoader(AgentPromptTemplateMapper mapper, PromptTemplateSeeder seeder) {
        this.mapper = mapper;
        this.seeder = seeder;
    }

    /**
     * 列出所有 ACTIVE 状态的 promptCode。
     *
     * @return 排序后的编码列表
     */
    public List<String> listActivePromptCodes() {
        return mapper.selectList(new LambdaQueryWrapper<AgentPromptTemplate>()
                .eq(AgentPromptTemplate::getStatus, "ACTIVE")
                .orderByAsc(AgentPromptTemplate::getPromptCode))
            .stream()
            .map(AgentPromptTemplate::getPromptCode)
            .toList();
    }

    /**
     * 加载指定 promptCode 的模板正文。
     *
     * @param promptCode 模板编码
     * @return 模板内容
     * @throws IllegalStateException 数据库与种子均缺失时
     */
    public String loadContent(String promptCode) {
        String normalized = PromptCodeResolver.resolve(promptCode);
        AgentPromptTemplate template = mapper.selectOne(new LambdaQueryWrapper<AgentPromptTemplate>()
            .eq(AgentPromptTemplate::getPromptCode, normalized)
            .eq(AgentPromptTemplate::getStatus, "ACTIVE")
            .last("LIMIT 1"));
        if (template != null) {
            return template.getContent();
        }
        PromptTemplateSeeder.DefaultPrompt fallback = seeder.defaults().get(normalized);
        if (fallback != null) {
            return fallback.content().trim();
        }
        throw new IllegalStateException("Missing prompt template: " + promptCode);
    }
}
