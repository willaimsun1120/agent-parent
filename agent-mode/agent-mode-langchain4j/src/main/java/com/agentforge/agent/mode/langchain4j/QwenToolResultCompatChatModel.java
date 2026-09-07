package com.agentforge.agent.mode.langchain4j;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageType;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import java.util.ArrayList;
import java.util.List;

/**
 * Qwen + LangChain4j 并行工具结果兼容包装。
 *
 * <p>Qwen 模型不支持连续多条 {@link ToolExecutionResultMessage}，本类将相邻工具结果
 * 合并为单条 {@link UserMessage} 后再委托给底层模型。
 */
public final class QwenToolResultCompatChatModel implements ChatLanguageModel {

    private final ChatLanguageModel delegate;

    /**
     * 构造兼容包装器。
     *
     * @param delegate 底层 ChatLanguageModel（通常为 QwenChatModel）
     */
    public QwenToolResultCompatChatModel(ChatLanguageModel delegate) {
        this.delegate = delegate;
    }

    /** {@inheritDoc} 发送前扁平化连续工具结果。 */
    @Override
    public Response<AiMessage> generate(List<ChatMessage> messages) {
        return delegate.generate(flattenConsecutiveToolResults(messages));
    }

    /** {@inheritDoc} 发送前扁平化连续工具结果。 */
    @Override
    public Response<AiMessage> generate(List<ChatMessage> messages, List<ToolSpecification> toolSpecifications) {
        return delegate.generate(flattenConsecutiveToolResults(messages), toolSpecifications);
    }

    /** {@inheritDoc} 发送前扁平化连续工具结果。 */
    @Override
    public Response<AiMessage> generate(List<ChatMessage> messages, ToolSpecification toolSpecification) {
        return delegate.generate(flattenConsecutiveToolResults(messages), toolSpecification);
    }

    /**
     * 将消息列表中连续的工具执行结果合并，供 Qwen 模型兼容处理。
     *
     * @param messages 原始消息列表
     * @return 扁平化后的消息列表
     */
    static List<ChatMessage> flattenConsecutiveToolResults(List<ChatMessage> messages) {
        List<ChatMessage> flattened = new ArrayList<>(messages.size());
        List<ToolExecutionResultMessage> pending = new ArrayList<>();

        for (ChatMessage message : messages) {
            if (message.type() == ChatMessageType.TOOL_EXECUTION_RESULT) {
                pending.add((ToolExecutionResultMessage) message);
                continue;
            }
            flushToolResults(flattened, pending);
            flattened.add(message);
        }
        flushToolResults(flattened, pending);
        return flattened;
    }

    /** 将累积的待处理工具结果刷入扁平列表：单条保留，多条合并为 UserMessage。 */
    private static void flushToolResults(List<ChatMessage> flattened, List<ToolExecutionResultMessage> pending) {
        if (pending.isEmpty()) {
            return;
        }
        if (pending.size() == 1) {
            flattened.add(pending.get(0));
        } else {
            flattened.add(mergeToolResults(pending));
        }
        pending.clear();
    }

    /** 将多条工具执行结果拼接为一条 UserMessage 文本。 */
    private static UserMessage mergeToolResults(List<ToolExecutionResultMessage> results) {
        StringBuilder builder = new StringBuilder("以下是本轮工具调用结果，请基于全部结果继续分析：\n");
        for (ToolExecutionResultMessage result : results) {
            builder.append("\n[").append(result.toolName()).append("]\n")
                .append(result.text())
                .append('\n');
        }
        return UserMessage.from(builder.toString().trim());
    }
}
