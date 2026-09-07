# hr-cs-example

HR 客服 Agent 可运行示例，演示人事场景下员工查询、知识库问答与写操作 HITL 的完整接入。接入方式与 `order-cs-example` 一致。

---

## 功能

- **员工查询** — 按 `EMP-xxxx` 格式工号查档案、请假、工资、社保、福利状态
- **知识库 RAG** — 请假制度、年假规则、工资发放、福利政策等文档检索（category 过滤）
- **多轮对话** — `sessionId` 串联上下文；缺工号时澄清追问
- **三种 Agent 模式** — manual / LangChain4j / AgentScope
- **HITL 写操作** — `SUBMIT_LEAVE`（提交请假）、`UPDATE_BENEFIT`（福利变更；平台 `orderNo` 字段存储 `empNo`）

---

## 配置

| 项 | 默认值 |
| --- | --- |
| 端口 | 8081 |
| 数据库 | `hr_agent_demo` |
| 向量库 | Milvus（collection: `hr_support_knowledge`） |
| biz-domain | `hr` |
| biz-module | `cs` |

向量库可通过 `VECTOR_STORE_TYPE=qdrant` 切换，连接参数见 `application.yml`。

---

## 启动

```bash
mvn spring-boot:run
```

访问 http://127.0.0.1:8081/admin/

---

## Demo 员工

| 工号 | 场景 |
| --- | --- |
| EMP-1001 | 年假充足，工资已发，福利正常 |
| EMP-1002 | 请假待审批，工资延迟，补充医疗待确认 |
| EMP-1003 | 请假被拒（余额不足），公积金未开通 |

---

## 示例问题

```text
我想请年假，怎么申请？                        → 无工号，触发澄清追问
员工 EMP-1001 还有多少天年假？                → 查员工 + 请假余额
EMP-1002 的工资为什么还没发？                  → 查工资 + 知识库
EMP-1003 的公积金为什么没开通？                → 查福利 + 知识库
帮 EMP-1001 提交 6 月 15 日开始的年假申请     → 创建 HITL 待确认操作
```

---

## 实现的 SPI

| SPI | 实现类 |
| --- | --- |
| `ManualAgentExecutor` | `HrManualAgentExecutor` |
| `FrameworkToolRegistrar` | `HrFrameworkToolRegistrar` + LangChain4j/AgentScope 适配器 |
| `ClarificationPolicy` | `HrClarificationPolicy` |
| `RagCategoryResolver` | `HrRagCategoryResolver` |
| `PromptTemplateSeeder` | `HrPromptTemplateSeeder`（`hr.cs.chat.*` 模板） |
| `ActionExecutor` | `SubmitLeaveActionExecutor` / `UpdateBenefitEnrollmentActionExecutor` |
| 知识库同步 | `KnowledgeIngestionService` |

---

## 常用 API

```bash
curl http://127.0.0.1:8081/api/employees
curl -X POST http://127.0.0.1:8081/api/knowledge/sync
curl -X POST http://127.0.0.1:8081/api/rag/eval

curl -X POST http://127.0.0.1:8081/api/agent/chat \
  -H 'Content-Type: application/json' \
  -d '{"question":"员工 EMP-1002 的工资为什么还没发？"}'

curl http://127.0.0.1:8081/api/agent/actions/pending
```

---

## 测试

```bash
# 在 agent-parent 目录
mvn test -pl agent-examples/hr-cs-example -am
```
