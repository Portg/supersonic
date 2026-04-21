---
status: implemented
module: common
key-files:
  - common/src/main/java/com/tencent/supersonic/common/outbox/OutboxPublisher.java
  - common/src/main/java/com/tencent/supersonic/common/outbox/OutboxRelay.java
  - common/src/main/java/com/tencent/supersonic/common/outbox/OutboxMapper.java
  - common/src/main/java/com/tencent/supersonic/common/outbox/OutboxEvent.java
  - common/src/main/java/com/tencent/supersonic/common/outbox/OutboxDeadEvent.java
  - launchers/standalone/src/main/resources/db/migration/mysql/V34__outbox.sql
  - launchers/standalone/src/main/resources/db/migration/postgresql/V34__outbox.sql
depends-on:
  - platform/02-rbac-tenant.md
---

# Transactional Outbox（P2-9）

## 背景

跨模块事件（如 `TemplateDeployedEvent`）原先通过 `ApplicationEventPublisher.publishEvent()` 直接投递，
与调用方事务不在同一原子域——若事务回滚，事件已发；若事件监听器失败，无重试。

本方案用关系型数据库行代替内存信道：将序列化后的事件持久化进 `s2_outbox`，
由独立 relay 线程轮询并重放为 Spring `ApplicationEvent`，实现 **at-least-once** 投递。

---

## 主链路

```
调用方 @Transactional
  └─ OutboxPublisher.publish(event)
        ├─ Jackson 序列化 → payloadJson
        └─ INSERT INTO s2_outbox

OutboxRelay（每 2 s，@Scheduled）
  └─ pollOnce()  ← @Transactional(REQUIRES_NEW)
        ├─ SELECT … FOR UPDATE SKIP LOCKED LIMIT 50
        ├─ 反序列化 → Class.forName(eventType)
        ├─ applicationEventPublisher.publishEvent(event)
        ├─ UPDATE s2_outbox SET processed_at = now()
        └─ 异常 → INSERT INTO s2_outbox_dead + 标记 processed_at

OutboxRetentionTask（每天 03:15）
  └─ DELETE FROM s2_outbox WHERE processed_at < now() - 7d
```

---

## 保证与边界

| 保证 | 说明 |
|------|------|
| **at-least-once** | relay 轮询，失败不删除（但目前无重试；failures 直接走 dead-letter） |
| **集群安全** | `FOR UPDATE SKIP LOCKED`：多个 relay 实例各自领取不重叠的行 |
| **事务一致** | 调用方回滚 → outbox 行也回滚，事件不投递 |
| **无 broker** | 仅依赖已有 RDBMS，无 Kafka/RabbitMQ 依赖 |
| **不保证顺序** | SKIP LOCKED 可能乱序；监听器须幂等 |
| **不保证恰好一次** | relay crash 后未提交的行会被重捞并重投 |

---

## 数据表

### s2_outbox

| 列 | 类型 | 说明 |
|----|------|------|
| id | BIGINT PK AUTO_INCREMENT | 行 ID |
| aggregate_type | VARCHAR(128) | 聚合根类型（如 `SemanticTemplate`） |
| aggregate_id | VARCHAR(64) | 聚合根 ID（可空） |
| event_type | VARCHAR(512) | 事件全限定类名 |
| payload_json | MEDIUMTEXT | Jackson 序列化后的 JSON |
| tenant_id | BIGINT | 投递时的租户 ID |
| created_at | DATETIME(3) | 写入时间 |
| processed_at | DATETIME(3) | relay 标记完成时间（NULL = 待处理） |
| processing_node | VARCHAR(256) | relay 节点（hostname）|
| attempts | INT | 投递尝试次数（当前 ≤1）|
| last_error | TEXT | 最近一次错误摘要 |

### s2_outbox_dead

relay 反序列化或投递失败时，将原始行复制到 `s2_outbox_dead` 并写入 `failure_reason`。
`original_id` 关联 `s2_outbox.id`（软引用）。

---

## 已迁移事件

| 事件类 | 触发位置 | 迁移版本 |
|--------|----------|---------|
| `TemplateDeployedEvent` | `SemanticTemplateServiceImpl.deployTemplate()` / `redeployTemplate()` | P2-9（V34） |

---

## 接入新事件

1. 事件类加无参构造器（Jackson 反序列化需要）+ `@JsonCreator` 或 `@JsonProperty` 注解。
2. `super(source)` 的 `source` 字段已通过 mixin 忽略，无需特殊处理。
3. 调用方注入 `OutboxPublisher`，将 `publishEvent(event)` 替换为 `outboxPublisher.publish(event)`。
4. 如果事件类在 `common` 模块以外，确认 relay 的 classpath 能找到该类（standalone 启动器包含全部模块，可直接使用）。

---

## 监控指标

| 指标 | 类型 | 含义 |
|------|------|------|
| `s2_outbox_unprocessed_count` | Gauge | 当前待处理行数（`processed_at IS NULL`） |
| `s2_outbox_lag_seconds` | Gauge | 最老待处理行的年龄（秒） |

两者均由 `OutboxMeterBinder` 通过 Micrometer 暴露，标签 `subsystem=outbox`。

告警建议：
- `s2_outbox_unprocessed_count > 500` 持续 5m → P2 告警
- `s2_outbox_lag_seconds > 60` 持续 5m → P1 告警（relay 可能挂死）

---

## 配置

```yaml
s2:
  outbox:
    enabled: true          # false = 降级为同步 publishEvent（不持久化）
    poll-interval-ms: 2000 # relay 轮询间隔
    batch-size: 50         # 每次 FOR UPDATE SKIP LOCKED 的行数
    retention-days: 7      # processed_at 行的 TTL（天）
    max-attempts: 5        # 保留字段，当前未实现重试
```

`enabled=false` 时 `OutboxPublisher` 退化为直接调用 `ApplicationEventPublisher`，其余 bean 不注册。

---

## 回滚方案

若需紧急回滚：
1. 将 `s2.outbox.enabled=false` 推送到线上配置，重启应用。
2. 调用方自动切回同步 `publishEvent`，`s2_outbox` 表停止写入。
3. 已在表中积压的行不影响业务；可后续手动标记 `processed_at` 清理或等 TTL 自动清除。
4. Flyway 迁移（V34）无需回滚——两张空表不影响任何其他功能。

---

## 已知风险

| 风险 | 缓解措施 |
|------|---------|
| 事件类移包/重命名 → `ClassNotFoundException` | `OutboxRelay` 捕获异常，写 dead-letter，不阻塞其他行 |
| relay 单点 | `FOR UPDATE SKIP LOCKED` 支持多实例并行，但单实例宕机期间有积压 |
| `payloadJson` 超 MEDIUMTEXT 上限（~16 MB） | 事件不应携带大 payload；如有需要改用对象存储引用 |
| at-least-once 导致重复消费 | 监听器须实现幂等；可通过 `OutboxEvent.id` 去重 |

---

## 后续演进

- **重试机制**：`attempts < maxAttempts` 时不移入 dead-letter，而是延迟重投（当前版本跳过）。
- **Kafka 替换**：relay 目标换成 `KafkaTemplate.send()`，调用方代码无需改动。
- **更多事件迁移**：`ModelChangedEvent`、`DataSetChangedEvent` 等跨模块事件可按"接入新事件"步骤逐步迁移。
