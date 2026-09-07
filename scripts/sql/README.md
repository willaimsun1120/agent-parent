# SQL 脚本

本目录提供两个 Demo 的**完整建库脚本**（由 Flyway 迁移按版本顺序合并生成），适用于：

- 快速初始化本地 MySQL，无需启动 Spring Boot
- DBA 审阅完整表结构与 Demo 数据
- CI / 集成测试环境一次性建库

> **日常开发推荐**仍使用 Flyway 自动迁移（启动 Demo 即可）。本目录脚本与 `agent-examples/*/src/main/resources/db/migration/` 保持同步，若迁移有变更需重新生成。

## 文件说明

| 文件 | 数据库 | 说明 |
| --- | --- | --- |
| [order_agent_demo_full.sql](order_agent_demo_full.sql) | `order_agent_demo` | 订单客服 Demo 完整脚本（V1~V11） |
| [hr_agent_demo_full.sql](hr_agent_demo_full.sql) | `hr_agent_demo` | HR 客服 Demo 完整脚本（V1~V5） |
| [all_demos_full.sql](all_demos_full.sql) | 以上两个库 | 一次性初始化两个 Demo |

## 使用方式

```bash
# 初始化订单 Demo
mysql -u root -p < scripts/sql/order_agent_demo_full.sql

# 初始化 HR Demo
mysql -u root -p < scripts/sql/hr_agent_demo_full.sql

# 一次性初始化两个 Demo
mysql -u root -p < scripts/sql/all_demos_full.sql
```

## 迁移来源

| Demo | 源目录 | 版本数 |
| --- | --- | --- |
| order-cs-example | `agent-examples/order-cs-example/src/main/resources/db/migration/` | 11 |
| hr-cs-example | `agent-examples/hr-cs-example/src/main/resources/db/migration/` | 5 |

## 重新生成

迁移脚本有更新时，在仓库根目录执行：

```bash
python3 scripts/sql/generate_full_sql.py
```
