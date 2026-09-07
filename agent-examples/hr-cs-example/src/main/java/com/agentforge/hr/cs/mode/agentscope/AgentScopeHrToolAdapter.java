package com.agentforge.hr.cs.mode.agentscope;

import com.agentforge.agent.runtime.AgentExecutionSupport;
import com.agentforge.agent.runtime.AgentRequestContextHolder;
import com.agentforge.agent.rag.CustomerServicePrompts;
import com.agentforge.agent.rag.KnowledgeDocument;
import com.agentforge.hr.cs.tool.HrTools;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AgentScopeHrToolAdapter {
    private static final Logger log = LoggerFactory.getLogger(AgentScopeHrToolAdapter.class);

    private final HrTools hrTools;
    private final List<String> toolCalls;
    private final AgentExecutionSupport.PreparedChat prepared;

    public AgentScopeHrToolAdapter(HrTools hrTools,
                                     List<String> toolCalls,
                                     AgentExecutionSupport.PreparedChat prepared) {
        this.hrTools = hrTools;
        this.toolCalls = toolCalls;
        this.prepared = prepared;
    }

    @Tool(name = "get_employee_detail", description = "根据员工工号查询员工、请假、工资、福利等完整业务事实。遇到 EMP- 开头的工号时优先使用。", readOnly = true)
    public String getEmployeeDetail(
        @ToolParam(name = "emp_no", description = "员工工号，例如 EMP-1001") String empNo) {
        return AgentRequestContextHolder.callWith(prepared, () -> {
            recordCall("get_employee_detail", "emp_no=" + empNo);
            return hrTools.getEmployeeDetail(empNo);
        });
    }

    @Tool(name = "get_leave_status", description = "根据员工工号查询年假余额与请假记录。", readOnly = true)
    public String getLeaveStatus(
        @ToolParam(name = "emp_no", description = "员工工号") String empNo) {
        return AgentRequestContextHolder.callWith(prepared, () -> {
            recordCall("get_leave_status", "emp_no=" + empNo);
            return hrTools.getLeaveStatus(empNo);
        });
    }

    @Tool(name = "get_payroll_status", description = "根据员工工号查询工资发放记录。", readOnly = true)
    public String getPayrollStatus(
        @ToolParam(name = "emp_no", description = "员工工号") String empNo) {
        return AgentRequestContextHolder.callWith(prepared, () -> {
            recordCall("get_payroll_status", "emp_no=" + empNo);
            return hrTools.getPayrollStatus(empNo);
        });
    }

    @Tool(name = "get_benefit_status", description = "根据员工工号查询社保与福利 enrollment 状态。", readOnly = true)
    public String getBenefitStatus(
        @ToolParam(name = "emp_no", description = "员工工号") String empNo) {
        return AgentRequestContextHolder.callWith(prepared, () -> {
            recordCall("get_benefit_status", "emp_no=" + empNo);
            return hrTools.getBenefitStatus(empNo);
        });
    }

    @Tool(name = "search_knowledge", description = "检索请假、工资、福利等客服知识库。回答前必须用原始问题检索一次规则依据。", readOnly = true)
    public String searchKnowledge(
        @ToolParam(name = "question", description = "客服原始问题") String question) {
        return AgentRequestContextHolder.callWith(prepared, () -> {
            recordCall("search_knowledge", "question=" + question);
            List<KnowledgeDocument> hits = hrTools.searchKnowledge(question, prepared.category());
            if (hits.isEmpty()) {
                return "知识库未命中相关规则。";
            }
            return CustomerServicePrompts.formatPolicyContext(hits);
        });
    }

    @Tool(name = "submit_leave_request", description = "创建待人工确认的请假申请。用户明确要求发起请假时使用。", readOnly = false)
    public String submitLeaveRequest(
        @ToolParam(name = "emp_no", description = "员工工号") String empNo,
        @ToolParam(name = "leave_type", description = "请假类型") String leaveType,
        @ToolParam(name = "days", description = "请假天数") int days,
        @ToolParam(name = "reason", description = "请假原因") String reason,
        @ToolParam(name = "start_date", description = "开始日期 yyyy-MM-dd") String startDate) {
        return AgentRequestContextHolder.callWith(prepared, () -> {
            recordCall("submit_leave_request", "emp_no=" + empNo);
            return hrTools.submitLeaveRequest(empNo, leaveType, days, reason, startDate);
        });
    }

    @Tool(name = "update_benefit_enrollment", description = "创建待人工确认的福利 enrollment 变更。", readOnly = false)
    public String updateBenefitEnrollment(
        @ToolParam(name = "emp_no", description = "员工工号") String empNo,
        @ToolParam(name = "benefit_type", description = "福利类型") String benefitType,
        @ToolParam(name = "target_status", description = "目标状态") String targetStatus) {
        return AgentRequestContextHolder.callWith(prepared, () -> {
            recordCall("update_benefit_enrollment", "emp_no=" + empNo);
            return hrTools.updateBenefitEnrollment(empNo, benefitType, targetStatus);
        });
    }

    private void recordCall(String toolName, String input) {
        toolCalls.add(toolName + "(" + input + ")");
    }
}
