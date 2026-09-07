-- 将初始三篇知识库正文优化为中文（便于 RAG 检索与客服 Agent 引用）

UPDATE knowledge_articles SET
    content = '【适用范围】所有已完成或进行中的订单退款咨询。

【基本规则】
1. 订单状态为 COMPLETED（已完成）的，自完成时间起 7 个自然日内可申请退款。
2. 超过 7 天的已完成订单，系统默认拒绝退款（refundStatus=REJECTED），常见拒绝原因：「订单完成已超过 7 天」。
3. 超过 7 天仍要退款，须客服主管审批后方可特批，不可直接告知用户「可以退」。

【不可退款情形】
- 支付回调缺失（paymentCallbackStatus 为空或异常）且订单仍停留在待回调状态（如 PENDING_PAYMENT_CALLBACK）时，应先修复订单状态，不得直接发起退款。
- 订单未完成、未支付成功的，走取消/关单流程，不走退款流程。

【客服话术要点】
- 先查订单完成时间 completedAt 与当前退款状态 refundStatus、拒绝原因 refundReason。
- 在 7 天内：可引导用户提交退款申请（需人工确认后写入退款表）。
- 超过 7 天：说明政策限制，如需特批请升级主管。

【关联字段】orderStatus、completedAt、refundStatus、refundReason、paymentStatus',
    version = version + 1,
    vector_synced_at = NULL
WHERE doc_code = 'refund-policy';

UPDATE knowledge_articles SET
    content = '【适用范围】用户反馈「已支付但订单状态不对」「支付成功页面未跳转」等支付异常。

【典型现象】
- paymentStatus = SUCCESS（渠道侧扣款成功）
- paymentCallbackStatus = FAILED（支付回调失败）
- orderStatus 仍停留在 PENDING_PAYMENT_CALLBACK（待支付回调）

【处理原则】
1. 先查订单号对应的支付记录与回调状态，不要凭用户口述判断。
2. 确认为「支付成功 + 回调失败」时，由运营/技术触发支付回调补偿（模拟环境可在后台点「支付回调成功」）。
3. 在确认支付渠道侧已有成功记录前，禁止引导用户重复支付。
4. 回调补偿成功后，订单状态应流转至正常后续状态（如待发货/已完成等，视业务而定）。

【排查步骤】
1. 查 paymentStatus、paymentCallbackStatus、orderStatus、paidAt。
2. 查是否有统一下单/支付请求日志（是否真正发起过支付）。
3. paymentStatus = PENDING 且无支付记录 → 用户未完成支付，非通道异常。
4. paymentStatus = FAILED → 结合失败原因排查（余额不足、超时、用户取消等）。

【客服话术要点】
- 支付已成功且仅回调延迟/失败：安抚用户，说明正在核实，勿重复付款。
- 未发起支付：引导用户在订单页重新支付。

【关联字段】paymentStatus、paymentCallbackStatus、orderStatus、paidAt、channel',
    version = version + 1,
    vector_synced_at = NULL
WHERE doc_code = 'payment-troubleshooting';

UPDATE knowledge_articles SET
    content = '【适用范围】会员/权益未到账、权益发放失败类咨询。

【基本规则】
1. 用户支付成功且订单正常完成后，应按会员等级发放对应权益（如 SVIP 月度礼包「SVIP monthly bonus」）。
2. 权益发放状态以 benefits 表为准，常见状态：PENDING（发放中）、ISSUED（已发放）、FAILED（失败）。

【权益失败处理】
1. 若 benefitStatus = FAILED，必须先查看 fail_reason（失败原因）。
2. 失败原因为「benefit service timeout」（权益服务超时）且 paymentStatus = SUCCESS 时：
   - 可为用户创建权益重试申请（retry_benefit_issue），经人工确认后执行补发。
   - 不可在未确认支付成功的情况下重试。
3. 非超时类失败（如资格不符、库存不足），需按具体原因解释，不可盲目重试。

【客服话术要点】
- 先确认订单已支付成功、订单状态是否已完成或处于可发放权益的状态。
- 权益 PENDING：说明正在处理，请稍后查看。
- 权益 FAILED + 超时：核实支付后可申请补发，需人工确认。

【关联字段】benefits、benefitStatus、fail_reason、paymentStatus、orderStatus、userNo/会员等级',
    version = version + 1,
    vector_synced_at = NULL
WHERE doc_code = 'member-benefits';
