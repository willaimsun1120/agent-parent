CREATE TABLE knowledge_articles (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    doc_code VARCHAR(64) NOT NULL UNIQUE,
    title VARCHAR(255) NOT NULL,
    category VARCHAR(64) NOT NULL,
    content TEXT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PUBLISHED',
    version INT NOT NULL DEFAULT 1,
    vector_synced_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_knowledge_articles_status (status),
    INDEX idx_knowledge_articles_category (category)
);

INSERT INTO knowledge_articles (doc_code, title, category, content, status, version) VALUES
('refund-policy', '退款政策', 'refund',
 'Orders can be refunded within 7 days after completion.\nCompleted orders older than 7 days are rejected unless customer-service manager approval is granted.\nRefunds are not allowed when the payment callback is missing because the order state must be repaired first.',
 'PUBLISHED', 1),
('payment-troubleshooting', '支付异常处理', 'payment',
 'When payment status is SUCCESS but callback_status is FAILED, the order may remain in PENDING_PAYMENT_CALLBACK.\nCustomer service should ask operations to trigger payment callback compensation.\nDo not ask the user to pay again before checking payment channel records.',
 'PUBLISHED', 1),
('member-benefits', '会员权益说明', 'benefit',
 'If benefit status is FAILED, check fail_reason first.\nFor benefit service timeout, customer service can retry benefit issuance after confirming payment success.\nSVIP users should receive monthly bonus benefits after a completed paid order.',
 'PUBLISHED', 1);
