package com.agentforge.hr.cs.mode;

import com.agentforge.agent.core.spi.FrameworkToolRegistrar;
import com.agentforge.agent.core.spi.ToolCallCollector;
import com.agentforge.agent.runtime.AgentExecutionSupport;
import com.agentforge.hr.cs.mode.agentscope.AgentScopeHrToolAdapter;
import com.agentforge.hr.cs.mode.langchain4j.LangChain4jHrToolAdapter;
import com.agentforge.hr.cs.tool.HrTools;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class HrFrameworkToolRegistrar implements FrameworkToolRegistrar {
    private static final Pattern EMP_NO_IN_QUESTION = Pattern.compile("EMP-\\d+");

    private final HrTools hrTools;

    public HrFrameworkToolRegistrar(HrTools hrTools) {
        this.hrTools = hrTools;
    }

    @Override
    public Object createLangChain4jTools(ToolCallCollector collector, ToolContext context) {
        return new LangChain4jHrToolAdapter(hrTools, collector.toolCalls(), toPreparedChat(context));
    }

    @Override
    public Object createAgentScopeTools(ToolCallCollector collector, ToolContext context) {
        return new AgentScopeHrToolAdapter(hrTools, collector.toolCalls(), toPreparedChat(context));
    }

    @Override
    public List<String> toolNamesHint() {
        return List.of("LangChain4j/AgentScope 工具：get_employee_detail/get_leave_status/get_payroll_status/get_benefit_status/search_knowledge/submit_leave_request/update_benefit_enrollment");
    }

    @Override
    public boolean expectsToolCalls(String question) {
        if (question == null || question.isBlank()) {
            return false;
        }
        return EMP_NO_IN_QUESTION.matcher(question).find()
            || question.contains("请假")
            || question.contains("年假")
            || question.contains("工资")
            || question.contains("社保")
            || question.contains("福利");
    }

    private AgentExecutionSupport.PreparedChat toPreparedChat(ToolContext context) {
        return new AgentExecutionSupport.PreparedChat(
            context.traceId(),
            context.sessionId(),
            context.turnIndex(),
            context.question(),
            context.category(),
            ""
        );
    }
}
