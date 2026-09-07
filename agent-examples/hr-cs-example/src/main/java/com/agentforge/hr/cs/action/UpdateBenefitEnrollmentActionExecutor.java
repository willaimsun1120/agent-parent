package com.agentforge.hr.cs.action;

import com.agentforge.agent.core.spi.ActionExecutionContext;
import com.agentforge.agent.core.spi.ActionExecutor;
import com.agentforge.hr.cs.cache.CachedEmployeeDetailService;
import com.agentforge.hr.cs.employee.HrBenefit;
import com.agentforge.hr.cs.employee.HrBenefitMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * 福利 enrollment 变更执行器。
 *
 * <p>平台 {@link ActionExecutionContext#orderNo()} 在 HR Demo 中存储 empNo。
 */
@Component
public class UpdateBenefitEnrollmentActionExecutor implements ActionExecutor {
    private static final Logger log = LoggerFactory.getLogger(UpdateBenefitEnrollmentActionExecutor.class);

    private final HrBenefitMapper hrBenefitMapper;
    private final CachedEmployeeDetailService cachedEmployeeDetailService;
    private final ObjectMapper objectMapper;

    public UpdateBenefitEnrollmentActionExecutor(HrBenefitMapper hrBenefitMapper,
                                                 CachedEmployeeDetailService cachedEmployeeDetailService,
                                                 ObjectMapper objectMapper) {
        this.hrBenefitMapper = hrBenefitMapper;
        this.cachedEmployeeDetailService = cachedEmployeeDetailService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String actionType() {
        return "UPDATE_BENEFIT";
    }

    @Override
    public void execute(ActionExecutionContext context) {
        Map<String, Object> payload = readPayload(context.payloadJson());
        String benefitType = stringValue(payload.get("benefitType"), null);
        String targetStatus = stringValue(payload.get("targetStatus"), "ACTIVE");
        if (benefitType == null || benefitType.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "benefitType is required");
        }
        HrBenefit existing = hrBenefitMapper.selectOne(new LambdaQueryWrapper<HrBenefit>()
            .eq(HrBenefit::getEmpNo, context.orderNo())
            .eq(HrBenefit::getBenefitType, benefitType)
            .last("LIMIT 1"));
        if (existing == null) {
            hrBenefitMapper.insert(new HrBenefit(context.orderNo(), benefitType, targetStatus));
        } else {
            existing.setStatus(targetStatus);
            hrBenefitMapper.updateById(existing);
        }
        cachedEmployeeDetailService.evict(context.orderNo());
        log.info("福利变更申请执行完成 actionId={} empNo={} benefitType={}",
            context.actionId(), context.orderNo(), benefitType);
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
}
