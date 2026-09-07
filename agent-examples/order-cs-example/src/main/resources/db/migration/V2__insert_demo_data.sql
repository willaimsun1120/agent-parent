INSERT INTO users (user_no, nickname, level_name) VALUES
('U1001', 'Alice', 'SVIP'),
('U1002', 'Bob', 'VIP'),
('U1003', 'Cindy', 'Normal');

INSERT INTO orders (order_no, user_no, status, amount, paid_at, completed_at) VALUES
('ORD-1001', 'U1001', 'COMPLETED', 199.00, '2026-06-01 10:00:00', '2026-06-01 10:10:00'),
('ORD-1002', 'U1002', 'PENDING_PAYMENT_CALLBACK', 59.00, '2026-06-08 09:00:00', NULL),
('ORD-1003', 'U1003', 'BENEFIT_FAILED', 99.00, '2026-06-08 11:30:00', '2026-06-08 11:45:00');

INSERT INTO payments (order_no, channel, status, callback_status, paid_at) VALUES
('ORD-1001', 'ALIPAY', 'SUCCESS', 'SUCCESS', '2026-06-01 10:00:00'),
('ORD-1002', 'WECHAT', 'SUCCESS', 'FAILED', '2026-06-08 09:00:00'),
('ORD-1003', 'ALIPAY', 'SUCCESS', 'SUCCESS', '2026-06-08 11:30:00');

INSERT INTO refunds (order_no, status, reason, requested_at) VALUES
('ORD-1001', 'REJECTED', 'Order completed more than 7 days ago.', '2026-06-08 12:00:00');

INSERT INTO benefits (user_no, order_no, benefit_name, status, fail_reason) VALUES
('U1001', 'ORD-1001', 'SVIP monthly bonus', 'ISSUED', NULL),
('U1002', 'ORD-1002', 'VIP coupon package', 'PENDING', NULL),
('U1003', 'ORD-1003', 'New user benefit', 'FAILED', 'Benefit service timeout.');
