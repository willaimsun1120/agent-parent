package com.agentforge.hr.cs.action;

import com.agentforge.agent.core.spi.ActionExecutionContext;
import com.agentforge.agent.core.spi.ActionExecutor;
import com.agentforge.hr.cs.cache.CachedEmployeeDetailService;
import com.agentforge.hr.cs.employee.LeaveRequest;
import com.agentforge.hr.cs.employee.LeaveRequestMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * 请假申请执行器。
 *
 * <p>平台 {@link ActionExecutionContext#orderNo()} 在 HR Demo 中存储 empNo。
 */
@Component
public class SubmitLeaveActionExecutor implements ActionExecutor {
    private static final Logger log = LoggerFactory.getLogger(SubmitLeaveActionExecutor.class);

    private final LeaveRequestMapper leaveRequestMapper;
    private final CachedEmployeeDetailService cachedEmployeeDetailService;
    private final ObjectMapper objectMapper;

    public SubmitLeaveActionExecutor(LeaveRequestMapper leaveRequestMapper,
                                     CachedEmployeeDetailService cachedEmployeeDetailService,
                                     ObjectMapper objectMapper) {
        this.leaveRequestMapper = leaveRequestMapper;
        this.cachedEmployeeDetailService = cachedEmployeeDetailService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String actionType() {
        return "SUBMIT_LEAVE";
    }

    @Override
    public void execute(ActionExecutionContext context) {
        Map<String, Object> payload = readPayload(context.payloadJson());
        String leaveType = stringValue(payload.get("leaveType"), "ANNUAL");
        int days = intValue(payload.get("days"), 1);
        String reason = stringValue(payload.get("reason"), "");
        LocalDate startDate = parseDate(stringValue(payload.get("startDate"), null));
        LeaveRequest leaveRequest = new LeaveRequest(
            context.orderNo(), leaveType, days, "PENDING", reason, startDate);
        leaveRequestMapper.insert(leaveRequest);
        cachedEmployeeDetailService.evict(context.orderNo());
        log.info("请假申请执行完成 actionId={} empNo={}", context.actionId(), context.orderNo());
    }

    private Map<String, Object> readPayload(String payloadJson) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = objectMapper.readValue(payloadJson == null ? "{}" : payloadJson, Map.class);
            return payload;
        } catch (JsonProcessingException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid payload JSON");
        }
    }

    private String stringValue(Object value, String defaultValue) {
        return value == null ? defaultValue : String.valueOf(value);
    }

    private int intValue(Object value, int defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return LocalDate.parse(value.trim());
    }
}
