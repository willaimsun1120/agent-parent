package com.agentforge.agent.prompt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** {@link PromptCode} 与 {@link PromptCodeResolver} 编解码测试。 */
class PromptCodeTest {

    /** 验证五段式编码的拼接、解析与面包屑标签。 */
    @Test
    void buildsAndParsesStructuredCode() {
        String code = PromptCode.of("order", "cs", "chat", "framework", "system");
        assertEquals("order.cs.chat.framework.system", code);

        PromptCode.Parsed parsed = PromptCode.parse(code);
        assertEquals("order", parsed.bizDomain());
        assertEquals("cs", parsed.bizModule());
        assertEquals("chat", parsed.scene());
        assertEquals("framework", parsed.agentMode());
        assertEquals("system", parsed.promptRole());
        assertTrue(parsed.breadcrumb().contains("订单"));
        assertTrue(parsed.breadcrumb().contains("框架模式"));
    }

    /** 验证旧版短别名可解析为新版五段式编码。 */
    @Test
    void resolvesLegacyAlias() {
        assertEquals(
            PromptTemplateCodes.ORDER_CS_CHAT_FRAMEWORK_SYSTEM,
            PromptCodeResolver.resolve("framework_system")
        );
    }

    /** 验证非法格式编码抛出异常。 */
    @Test
    void rejectsInvalidCode() {
        assertThrows(IllegalArgumentException.class, () -> PromptCode.parse("framework_system"));
    }
}
