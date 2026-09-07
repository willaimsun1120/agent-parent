package com.agentforge.agent.core.spi;

import java.util.Map;

/**
 * 默认提示词模板种子 SPI。
 *
 * <p>应用首次启动时由平台调用，将业务预置的 prompt 写入模板存储。
 */
public interface PromptTemplateSeeder {

    /**
     * @return 模板名 → 默认内容的映射
     */
    Map<String, DefaultPrompt> defaults();

    /**
     * 单条默认提示词模板。
     *
     * @param name        模板名称（唯一键）
     * @param description 用途说明
     * @param content     模板正文
     */
    record DefaultPrompt(String name, String description, String content) {
    }
}
