# agent-examples

业务 Agent 示例聚合模块。每个子模块实现平台 SPI，演示一种业务场景从表结构、工具、知识库到 HITL 的完整接入。

平台架构、SPI 契约与基础设施说明见 [../README.md](../README.md)。

---

## 前置条件

运行 Demo 前请先完成：

1. **MySQL 8** — 创建对应数据库（见下方各 Demo）
2. **Docker** — 启动向量库等依赖（在本目录 `../docker-compose.yml`）
3. **DashScope API Key**（可选）— `export DASHSCOPE_API_KEY=...`，未配置时 manual 模式使用本地模板回答

```bash
# 在 agent-parent 目录
docker compose up -d qdrant redis          # 订单 Demo（默认 Qdrant）
docker compose up -d milvus redis          # HR Demo（默认 Milvus）
```

---

## 快速运行

两个 Demo **相互独立**，可只启动其中一个，也可同时运行（端口不同）。

### 订单客服 — `order-cs-example`

```bash
# 建库（首次）
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS order_agent_demo DEFAULT CHARACTER SET utf8mb4;"

cd order-cs-example
mvn spring-boot:run
```

| 项 | 值 |
| --- | --- |
| 端口 | 8080 |
| Admin | http://127.0.0.1:8080/admin/ |
| 向量库 | Qdrant（`order_support_knowledge`） |

### HR 客服 — `hr-cs-example`

```bash
# 建库（首次）
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS hr_agent_demo DEFAULT CHARACTER SET utf8mb4;"

cd hr-cs-example
mvn spring-boot:run
```

| 项 | 值 |
| --- | --- |
| 端口 | 8081 |
| Admin | http://127.0.0.1:8081/admin/ |
| 向量库 | Milvus（`hr_support_knowledge`） |

### 统一管理控制台（可选）

Vue 3 前端聚合两个 Demo，适合本地联调：

```bash
cd admin-console
npm install && npm run dev    # http://127.0.0.1:8090
```

需同时启动对应的后端服务；各 Demo 自带的 `/admin/` 静态页不依赖此前端。

---

## 业务 Demo 一览

功能细节、Demo 数据、SPI 实现与示例问题，见各子模块 README：

| Demo | 模块 | 业务场景 | 详细说明 |
| --- | --- | --- | --- |
| 订单客服 | `order-cs-example` | 订单查询、支付/退款/权益、知识库 RAG、HITL 退款与权益补发 | [order-cs-example/README.md](order-cs-example/README.md) |
| HR 客服 | `hr-cs-example` | 员工查询、请假/年假/工资/福利、知识库 RAG、HITL 请假与福利变更 | [hr-cs-example/README.md](hr-cs-example/README.md) |

---

## 测试

```bash
# 在 agent-parent 目录

# 订单 Demo
mvn test -pl agent-examples/order-cs-example -am

# HR Demo
mvn test -pl agent-examples/hr-cs-example -am
```
