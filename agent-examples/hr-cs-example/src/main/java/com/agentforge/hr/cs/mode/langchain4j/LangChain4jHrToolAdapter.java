package com.agentforge.hr.cs.mode.langchain4j;

import com.agentforge.agent.runtime.AgentExecutionSupport;
import com.agentforge.agent.runtime.AgentRequestContextHolder;
import com.agentforge.agent.rag.CustomerServicePrompts;
import com.agentforge.agent.rag.KnowledgeDocument;
import com.agentforge.hr.cs.tool.HrTools;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LangChain4jHrToolAdapter {
    private static final Logger log = LoggerFactory.getLogger(LangChain4jHrToolAdapter.class);

    private final HrTools hrTools;
    private final List<String> toolCalls;
    private final AgentExecutionSupport.PreparedChat prepared;

    public LangChain4jHrToolAdapter(HrTools hrTools,
                                      List<String> toolCalls,
                                      AgentExecutionSupport.PreparedChat prepared) {
        this.hrTools = hrTools;
        this.toolCalls = toolCalls;
        this.prepared = prepared;
    }

    @Tool(name = "get_employee_detail", value = "根据员工工号查询员工、请假、工资、福利等完整业务事实。遇到 EMP- 开头的工号时优先使用。")
    public String getEmployeeDetail(@P("员工工号，例如 EMP-1001") String empNo) {
        return AgentRequestContextHolder.callWith(prepared, () -> {
            recordCall("get_employee_detail", "emp_no=" + empNo);
            return hrTools.getEmployeeDetail(empNo);
        });
    }

    @Tool(name = "get_leave_status", value = "根据员工工号查询年假余额与请假记录。用户询问请假、年假时使用。")
    public String getLeaveStatus(@P("员工工号，例如 EMP-1001") String empNo) {
        return AgentRequestContextHolder.callWith(prepared, () -> {
            recordCall("get_leave_status", "emp_no=" + empNo);
            return hrTools.getLeaveStatus(empNo);
        });
    }

    @Tool(name = "get_payroll_status", value = "根据员工工号查询工资发放记录。用户询问工资、发薪时使用。")
    public String getPayrollStatus(@P("员工工号，例如 EMP-1002") String empNo) {
        return AgentRequestContextHolder.callWith(prepared, () -> {
            recordCall("get_payroll_status", "emp_no=" + empNo);
            return hrTools.getPayrollStatus(empNo);
        });
    }

    @Tool(name = "get_benefit_status", value = "根据员工工号查询社保与福利 enrollment 状态。用户询问社保、福利时使用。")
    public String getBenefitStatus(@P("员工工号，例如 EMP-1003") String empNo) {
        return AgentRequestContextHolder.callWith(prepared, () -> {
            recordCall("get_benefit_status", "emp_no=" + empNo);
            return hrTools.getBenefitStatus(empNo);
        });
    }

    @Tool(name = "search_knowledge", value = "检索请假、工资、福利等客服知识库。回答前必须用原始问题检索一次规则依据。")
    public String searchKnowledge(@P("客服原始问题") String question) {
        return AgentRequestContextHolder.callWith(prepared, () -> {
            recordCall("search_knowledge", "question=" + question);
            List<KnowledgeDocument> hits = hrTools.searchKnowledge(question);
            return formatHits(hits);
        });
    }

    @Tool(name = "submit_leave_request", value = "创建待人工确认的请假申请。用户明确要求发起请假时使用，不会直接写入请假表。")
    public String submitLeaveRequest(@P("员工工号") String empNo,
                                     @P("请假类型，如 ANNUAL") String leaveType,
                                     @P("请假天数") int days,
                                     @P("请假原因") String reason,
                                     @P("开始日期，格式 yyyy-MM-dd") String startDate) {
        return AgentRequestContextHolder.callWith(prepared, () -> {
            recordCall("submit_leave_request", "emp_no=" + empNo);
            return hrTools.submitLeaveRequest(empNo, leaveType, days, reason, startDate);
        });
    }

    @Tool(name = "update_benefit_enrollment", value = "创建待人工确认的福利 enrollment 变更。用户明确要求变更福利时使用。")
    public String updateBenefitEnrollment(@P("员工工号") String empNo,
                                          @P("福利类型") String benefitType,
                                          @P("目标状态，如 ACTIVE") String targetStatus) {
        return AgentRequestContextHolder.callWith(prepared, () -> {
            recordCall("update_benefit_enrollment", "emp_no=" + empNo);
            return hrTools.updateBenefitEnrollment(empNo, benefitType, targetStatus);
        });
    }

    private String formatHits(List<KnowledgeDocument> hits) {
        if (hits.isEmpty()) {
            return "知识库未命中相关规则。";
        }
        return CustomerServicePrompts.formatPolicyContext(hits);
    }

    private void recordCall(String toolName, String input) {
        toolCalls.add(toolName + "(" + input + ")");
    }
}
