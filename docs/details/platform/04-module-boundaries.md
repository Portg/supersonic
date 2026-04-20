---
status: active
module: platform
key-files:
  - launchers/standalone/src/test/java/com/tencent/supersonic/archunit/ModuleBoundaryTest.java
  - common/pom.xml
depends-on: []
---

# 模块边界强制（ArchUnit）

> 主文档：[智能运营数据中台设计方案](../../智能运营数据中台设计方案.md)
> 相关重构：[headless DTO boundary migration](../../superpowers/plans/2026-04-14-headless-dto-boundary-migration.md)

## 1. 为什么需要这些规则

SuperSonic 曾多次出现模块边界被破坏的情况：MyBatis DO 被错误地放进 `.api` 模块；
`feishu.server` 直接 import `headless.server` 的类；`chat.server` 绕过 `headless.api`
直接依赖 `headless.server`。这些问题都在上线后才被发现，回退成本高。

ArchUnit 在 `mvn test` 阶段就让构建失败，把"模块边界"从团队约定变成编译期约束。

## 2. 当前规则清单

所有规则写在 `launchers/standalone/src/test/java/com/tencent/supersonic/archunit/ModuleBoundaryTest.java`。

| # | 规则 | 目的 |
|---|------|------|
| 1 | `headless.api` 禁依赖 `headless.server` | DTO/契约不能反向依赖持久化实现（Rule 2 的子集，保留用于文档可发现性） |
| 2 | 所有 `*.api` 禁依赖 `*.server` / `auth.authentication` / `auth.authorization` | 通用版本，适用于 chat/feishu/billing/headless；auth 用 authentication/authorization 作为实现包 |
| 3 | `auth.api` 禁依赖 `auth.authentication` / `auth.authorization` | auth 模块契约分离 |
| 4 | `chat.server` 禁依赖 `headless.server` | chat 只能通过 headless-api/headless-chat/headless-core 消费能力 |
| 5 | `headless.chat` 禁依赖 `headless.server` | 语义解析逻辑不能反向依赖服务端持久化实现 |
| 6 | `feishu.server` 禁依赖其他模块的 server 内部实现 | 飞书作为投递渠道，只能消费公开 `.api` 契约（黑名单：chat/headless/billing/auth 实现层） |
| 7 | `chat.server` 禁依赖 `feishu.server` | 跨模块通知走 Spring `ApplicationEvent` |
| 8 | `common` 禁依赖任何同级模块 | `common` 必须是纯基础设施 |
| 9 | `*.api` 类禁用 Spring `@Component/@Service/@Repository/@Controller/@RestController/@Configuration/@ControllerAdvice/@RestControllerAdvice` | API 是纯契约，不能携带 Spring 托管逻辑 |
| 10 | 顶级模块切片间无循环依赖 | 由 `slices().beFreeOfCycles()` 兜底 |

## 3. 运行方式

本地：
```bash
mvn -pl launchers/standalone -am -Dtest=ArchUnitSmokeTest,ModuleBoundaryTest -Dsurefire.failIfNoSpecifiedTests=false test
```

CI：`.github/workflows/ubuntu-ci.yml` 在 push / PR 到 `master` 时执行 `mvn test`，
覆盖所有模块（含 `launchers-standalone` 的 `ModuleBoundaryTest`）。无需额外 workflow 配置。

## 4. 违规时怎么办

1. **读 ArchUnit 输出。** 它会打印每个违规类 + 禁止依赖的类，精确到行号。
2. **修复而不是绕过。** 99% 的违规是真 Bug。按模块边界把类移到正确的模块。
3. **只在批量遗留违规时才 freeze。** 使用 `FreezingArchRule.freeze(rule)` 临时冻结（`archunit_store/` 目录），并立刻开清理 ticket。

## 5. 如何新增规则

在 `ModuleBoundaryTest.java` 里追加一个 `@ArchTest static final ArchRule …` 字段。

### 禁止某模块依赖另一模块

```java
@ArchTest
static final ArchRule xxx_shouldNotDependOn_yyy =
        noClasses()
                .that().resideInAPackage("..xxx..")
                .should().dependOnClassesThat().resideInAPackage("..yyy..")
                .because("<明确的一句理由>");
```

### 新增 `*.server` 模块时

更新规则 #2 的 `resideInAnyPackage(...)` 列表，加入 `..<new>.server..`。

### 注意事项

规则 #8（`common` 禁依赖同级模块）使用完整包名 `com.tencent.supersonic.*` 而非
`..auth..` 等短模式，以避免误匹配第三方库（如 `software.amazon.awssdk.auth.*`）。
新增类似规则时请遵循同样做法。
