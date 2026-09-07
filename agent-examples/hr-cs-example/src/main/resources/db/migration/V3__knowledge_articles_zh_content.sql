INSERT INTO knowledge_articles (doc_code, title, category, content, status, version) VALUES
('leave-policy', '请假与年假政策', 'leave',
 '员工年假按入职年限计算，每年 1 月 1 日更新余额。\n年假余额不足时，年假类请假申请将被拒绝；可使用事假或调休替代。\n请假需提前 3 个工作日提交申请，3 天以内由直属主管审批，3 天以上需 HR 复核。\n已批准的请假可在系统中查询状态；待审批申请一般 1-2 个工作日内处理。',
 'PUBLISHED', 1),
('payroll-faq', '工资发放常见问题', 'payroll',
 '工资于每月 10 日前发放至员工绑定的银行卡。\n若状态显示 DELAYED（延迟发放），常见原因为：考勤异常待确认、个税信息变更、或银行账号有误。\n客服应先核对 payroll 记录中的 pay_month 与 status，再联系 HR 薪酬组核实。\n请勿重复提交发薪申请；确认账号无误后等待补发即可。',
 'PUBLISHED', 1),
('benefit-guide', '社保与福利指南', 'benefit',
 '入职后公司统一缴纳五险一金；补充医疗保险需在入职 30 天内完成 enrollment。\nbenefit_type 为 SUPPLEMENTARY_MEDICAL 时，PENDING 表示待员工在福利平台确认。\nINACTIVE 表示未开通或已停保；ACTIVE 表示正常生效。\n员工要求变更福利 enrollment 时，需创建待确认申请，由 HR 人工审核后生效。',
 'PUBLISHED', 1);
