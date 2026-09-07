# order-cs-example

订单客服 Agent 可运行示例，演示电商场景下订单查询、知识库问答与写操作 HITL 的完整接入。

---

## 功能

- **订单查询** — 按 `ORD-xxxx` 格式订单号查详情、支付、退款、权益状态
- **知识库 RAG** — 退款规则、支付异常、会员权益等文档检索（category 过滤）
- **多轮对话** — `sessionId` 串联上下文；缺订单号时澄清追问
- **三种 Agent 模式** — manual / LangChain4j / AgentScope（框架模式失败可降级 manual）
- **HITL 写操作** — `SUBMIT_REFUND`（提交退款申请）、`RETRY_BENEFIT`（权益补发重试）
- **运营能力** — Admin 页面维护订单、知识库、提示词；模拟下单；RAG 评测

---

## 配置

| 项 | 默认值 |
| --- | --- |
| 端口 | 8080 |
| 数据库 | `order_agent_demo` |
| 向量库 | Qdrant（collection: `order_support_knowledge`） |
| biz-domain | `order` |
| biz-module | `cs` |

向量库可通过 `VECTOR_STORE_TYPE=milvus` 切换，连接参数见 `application.yml`。

---

## 启动

```bash
mvn spring-boot:run
```

访问 http://127.0.0.1:8080/admin/

---

## Demo 订单

| 订单号 | 场景 |
| --- | --- |
| ORD-1001 | 已完成，支付成功；退款被拒（超过 7 天） |
| ORD-1002 | 支付回调异常，订单状态 `PENDING_PAYMENT_CALLBACK` |
| ORD-1003 | 支付成功，会员权益发放失败（`BENEFIT_FAILED`） |

---

## 示例问题

```text
这个订单为什么不能退款？                      → 无订单号，触发澄清追问
订单 ORD-1001 为什么不能退款？              → 查订单 + 退款状态 + RAG
订单 ORD-1002 支付成功但状态异常，怎么处理？  → 查支付 + 知识库
订单 ORD-1003 会员权益为什么没到账？          → 查权益 + 知识库
帮 ORD-1001 提交退款申请，原因是用户误购       → 创建 HITL 待确认操作
```

---

## 实现的 SPI

| SPI | 实现类 |
| --- | --- |
| `ManualAgentExecutor` | `OrderManualAgentExecutor` |
| `FrameworkToolRegistrar` | `OrderFrameworkToolRegistrar` + LangChain4j/AgentScope 适配器 |
| `ClarificationPolicy` | `OrderClarificationPolicy` |
| `RagCategoryResolver` | `OrderRagCategoryResolver` |
| `PromptTemplateSeeder` | `OrderPromptTemplateSeeder`（`order.cs.chat.*` 模板） |
| `ActionExecutor` | `SubmitRefundActionExecutor` / `RetryBenefitActionExecutor` |
| 知识库同步 | `KnowledgeIngestionService` |

---

## 常用 API

```bash
curl http://127.0.0.1:8080/api/orders
curl -X POST http://127.0.0.1:8080/api/knowledge/sync
curl -X POST http://127.0.0.1:8080/api/rag/eval

curl -X POST http://127.0.0.1:8080/api/agent/chat \
  -H 'Content-Type: application/json' \
  -d '{"question":"订单 ORD-1002 支付成功但状态异常，怎么处理？"}'

curl http://127.0.0.1:8080/api/agent/actions/pending
```

---

## 测试

```bash
# 在 agent-parent 目录
mvn test -pl agent-examples/order-cs-example -am
```
