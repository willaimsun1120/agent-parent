package com.agentforge.hr.cs.employee;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("hr_benefits")
public class HrBenefit {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String empNo;
    private String benefitType;
    private String status;
    private LocalDateTime createdAt;

    public HrBenefit() {
    }

    public HrBenefit(String empNo, String benefitType, String status) {
        this.empNo = empNo;
        this.benefitType = benefitType;
        this.status = status;
    }

    public Long getId() { return id; }
    public String getEmpNo() { return empNo; }
    public String getBenefitType() { return benefitType; }
    public String getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setId(Long id) { this.id = id; }
    public void setEmpNo(String empNo) { this.empNo = empNo; }
    public void setBenefitType(String benefitType) { this.benefitType = benefitType; }
    public void setStatus(String status) { this.status = status; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
