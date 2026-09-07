INSERT INTO employees (emp_no, name, department, annual_leave_balance, hire_date) VALUES
('EMP-1001', '张三', '研发部', 10, '2022-03-15'),
('EMP-1002', '李四', '市场部', 2, '2023-08-01'),
('EMP-1003', '王五', '财务部', 0, '2020-01-10');

INSERT INTO leave_requests (emp_no, leave_type, days, status, reason, start_date) VALUES
('EMP-1001', 'ANNUAL', 3, 'APPROVED', '家庭事务', '2026-07-01'),
('EMP-1002', 'ANNUAL', 2, 'PENDING', '个人休息', '2026-06-20'),
('EMP-1003', 'ANNUAL', 5, 'REJECTED', '年假余额不足', '2026-06-15');

INSERT INTO payroll_records (emp_no, pay_month, amount, status, paid_at) VALUES
('EMP-1001', '2026-05', 18000.00, 'PAID', '2026-05-10 10:00:00'),
('EMP-1002', '2026-05', 12000.00, 'DELAYED', NULL),
('EMP-1003', '2026-05', 15000.00, 'PAID', '2026-05-10 10:00:00');

INSERT INTO hr_benefits (emp_no, benefit_type, status) VALUES
('EMP-1001', 'SOCIAL_INSURANCE', 'ACTIVE'),
('EMP-1001', 'SUPPLEMENTARY_MEDICAL', 'ACTIVE'),
('EMP-1002', 'SOCIAL_INSURANCE', 'ACTIVE'),
('EMP-1002', 'SUPPLEMENTARY_MEDICAL', 'PENDING'),
('EMP-1003', 'SOCIAL_INSURANCE', 'ACTIVE'),
('EMP-1003', 'HOUSING_FUND', 'INACTIVE');
