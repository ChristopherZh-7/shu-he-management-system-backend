# 工单中心 - 设计补全文档（Step A）

> **文档定位**：在 MCP-2 已交付的「表结构 + 菜单 SQL + 模块骨架」基础上，补齐 4 个关键决策点，让 Step B（API 契约）与 Step C（代码实现）有据可依。
>
> 配套产物：
>
> - 表结构：`sql/mysql/migration/V2026_05_19_01__add_ticket_tables.sql`
> - 菜单权限：`sql/mysql/migration/V2026_05_19_02__add_ticket_module_menu.sql`
> - 模块壳子：`shuhe-module-ticket/`（仅 `pom.xml` + `package-info.java`）

---

## 1. 工单状态机

### 1.1 状态枚举

| 值 | 枚举常量 | 名称 | 说明 |
|---|---|---|---|
| `0` | `PENDING` | 待处理 | 已创建未分派 / 已分派未接手 |
| `1` | `IN_PROGRESS` | 处理中 | 处理人已开始工作 |
| `2` | `PENDING_REVIEW` | 待审核 | 处理人提交结果，等审核（可选环节）|
| `3` | `COMPLETED` | 已完成 | 处理完毕，等待提单人确认 |
| `4` | `CLOSED` | 已关闭 | 提单人确认 / 超时自动关闭 |
| `5` | `CANCELLED` | 已取消 | 提单人撤回 / 超时未受理 |

### 1.2 合法转换矩阵

`✓` 允许 / `✗` 禁止；表头为 to，左侧为 from：

| from \ to | 0 待处理 | 1 处理中 | 2 待审核 | 3 已完成 | 4 已关闭 | 5 已取消 |
|---|---|---|---|---|---|---|
| **0 待处理** | — | ✓ | ✗ | ✗ | ✗ | ✓ |
| **1 处理中** | ✓ (取回) | — | ✓ | ✓ (无审核) | ✗ | ✓ (申请) |
| **2 待审核** | ✗ | ✓ (驳回) | — | ✓ (通过) | ✗ | ✗ |
| **3 已完成** | ✓ (reopen) | ✗ | ✗ | — | ✓ | ✗ |
| **4 已关闭** | ✓ (reopen) | ✗ | ✗ | ✗ | — | ✗ |
| **5 已取消** | ✗ | ✗ | ✗ | ✗ | ✗ | — |

### 1.3 动作（action）与状态变化的映射

| action | from | to | 操作人 | 备注 |
|---|---|---|---|---|
| `create` | — | `0` | 任意有 `ticket:ticket:create` 的用户 | 自动写一条 log |
| `assign` | `0` | `0` | 部门负责人 / 工单管理员 / 超管 | 改 `assignee_id`，状态不变；记 log |
| `start` | `0` | `1` | `assignee_id` 本人 | 记 log + 写 `first_response_time` |
| `submit_review` | `1` | `2` | `assignee_id` 本人 | 仅当工单分类配置了「需审核」时可用 |
| `review_pass` | `2` | `3` | 审核人（默认 = 提单人的部门负责人）| 记 log |
| `review_reject` | `2` | `1` | 同上 | 记 log + comment |
| `finish` | `1` | `3` | `assignee_id` 本人（无审核分支） | 记 log + 写 `finish_time` |
| `close` | `3` | `4` | 提单人 / 工单管理员 / 超时定时任务 | 记 log + 写 `close_time` |
| `reopen` | `3` 或 `4` | `1` | 工单管理员 / 超管 | 记 log；保留原 `assignee_id` |
| `cancel` | `0` | `5` | 提单人 | 记 log |
| `cancel` | `1` | `5` | 提单人申请 + assignee 同意（先 comment 后 cancel） | 二期：实现「申请取消」流程；一期：仅允许 `status=0` 取消 |
| `transfer` | 任意非终态 | 同 | 当前 assignee / 部门负责人 / 工单管理员 | 改 `assignee_id`，记 log（from/to assignee 都写）|
| `comment` | 任意 | 同 | 提单人 / assignee / 部门负责人 / 工单管理员 | 不记 log（评论本身在 `shuhe_ticket_comment` 表）|

### 1.4 实现约束（必落到 Service 层代码）

1. **状态机校验集中在一个工具类** `TicketStateMachine.checkTransition(from, to, action)`，所有状态变更操作（assign / start / finish / close ...）的第一步都调它。
2. **transition 错误码统一返 `TICKET_STATUS_INVALID`**，message 模板：`"工单当前状态={fromName}，不允许{actionName}操作"`。
3. **每一次状态变化必写 log**：写 `shuhe_ticket_log` 是 transition 的副作用，封装在 Service 私有方法 `writeLog(ticketId, action, fromStatus, toStatus, ...)`，所有 action 方法都调它，禁止跳过。
4. **MVP 范围**：一期实现 create / start / finish / close / cancel / assign / transfer / comment 8 个 action；二期补 submit_review / review_pass / review_reject / 申请取消流程。

---

## 2. 错误码（号段 1_032_xxx_xxx）

> **号段分配**：参考 `shuhe-module-project` 的 `1_030_000_000` 段，本模块占用 **`1_032_000_000`**（紧接 `1_031` finance）。

### 2.1 子段位规划

| 子段 | 用途 |
|---|---|
| `1_032_001_xxx` | 工单主表通用错误 |
| `1_032_002_xxx` | 工单分类相关 |
| `1_032_003_xxx` | 状态机相关 |
| `1_032_004_xxx` | 评论相关 |
| `1_032_005_xxx` | 附件相关 |
| `1_032_006_xxx` | 权限 / 数据权限相关 |
| `1_032_007_xxx` | 事件适配 / 业务联动（二期） |
| `1_032_008_xxx` | 通知（站内信 / 钉钉，二期） |

### 2.2 一期需要的错误码（全清单）

```java
public interface ErrorCodeConstants {

    // ========== 工单主表 1_032_001_xxx ==========
    ErrorCode TICKET_NOT_EXISTS              = new ErrorCode(1_032_001_000, "工单不存在");
    ErrorCode TICKET_NO_DUPLICATE            = new ErrorCode(1_032_001_001, "工单编号重复");
    ErrorCode TICKET_NO_GENERATE_FAIL        = new ErrorCode(1_032_001_002, "工单编号生成失败");
    ErrorCode TICKET_TITLE_EMPTY             = new ErrorCode(1_032_001_003, "工单标题不能为空");
    ErrorCode TICKET_DEPT_REQUIRED           = new ErrorCode(1_032_001_004, "工单必须指定归属部门");
    ErrorCode TICKET_CREATOR_DEPT_MISSING    = new ErrorCode(1_032_001_005, "提单人未配置所属部门，无法创建工单");

    // ========== 工单分类 1_032_002_xxx ==========
    ErrorCode TICKET_CATEGORY_NOT_EXISTS     = new ErrorCode(1_032_002_000, "工单分类不存在");
    ErrorCode TICKET_CATEGORY_DISABLED       = new ErrorCode(1_032_002_001, "工单分类已禁用");
    ErrorCode TICKET_CATEGORY_HAS_CHILDREN   = new ErrorCode(1_032_002_002, "分类下存在子分类，无法删除");
    ErrorCode TICKET_CATEGORY_HAS_TICKETS    = new ErrorCode(1_032_002_003, "分类下存在工单，无法删除");
    ErrorCode TICKET_CATEGORY_PARENT_INVALID = new ErrorCode(1_032_002_004, "父分类不能选择自己或自己的子分类");

    // ========== 状态机 1_032_003_xxx ==========
    ErrorCode TICKET_STATUS_INVALID          = new ErrorCode(1_032_003_000, "工单当前状态不允许该操作");
    ErrorCode TICKET_ASSIGNEE_REQUIRED       = new ErrorCode(1_032_003_001, "请先分派处理人");
    ErrorCode TICKET_ASSIGNEE_SAME           = new ErrorCode(1_032_003_002, "目标处理人与当前处理人相同");
    ErrorCode TICKET_ASSIGNEE_NOT_EXISTS     = new ErrorCode(1_032_003_003, "目标处理人不存在或已离职");
    ErrorCode TICKET_REOPEN_FORBIDDEN        = new ErrorCode(1_032_003_004, "工单已关闭超过 30 天，不允许重新打开");

    // ========== 评论 1_032_004_xxx ==========
    ErrorCode TICKET_COMMENT_NOT_EXISTS      = new ErrorCode(1_032_004_000, "评论不存在");
    ErrorCode TICKET_COMMENT_EMPTY           = new ErrorCode(1_032_004_001, "评论内容不能为空");
    ErrorCode TICKET_COMMENT_INTERNAL_DENY   = new ErrorCode(1_032_004_002, "无权查看内部评论");

    // ========== 附件 1_032_005_xxx ==========
    ErrorCode TICKET_ATTACHMENT_NOT_EXISTS   = new ErrorCode(1_032_005_000, "附件不存在");
    ErrorCode TICKET_ATTACHMENT_SIZE_LIMIT   = new ErrorCode(1_032_005_001, "附件大小超出限制（默认 20MB）");
    ErrorCode TICKET_ATTACHMENT_TYPE_DENIED  = new ErrorCode(1_032_005_002, "附件类型不允许（仅支持文档/图片）");

    // ========== 权限 1_032_006_xxx ==========
    ErrorCode TICKET_NO_PERMISSION           = new ErrorCode(1_032_006_000, "无权操作该工单");
    ErrorCode TICKET_NOT_OWN                 = new ErrorCode(1_032_006_001, "不是您的工单，无权修改");
    ErrorCode TICKET_NOT_ASSIGNEE            = new ErrorCode(1_032_006_002, "您不是当前处理人，无法执行该操作");
}
```

### 2.3 前端错误码翻译策略

- HTTP 状态码：业务错误统一 `200 + body.code`；HTTP 5xx 仅用于「框架崩溃 / 网关错误」。
- 前端 axios 拦截器：根据 `body.code` 在 `errorMessages.ts` 里查 map；映射不到时回退用 `body.msg`。
- 表单字段级错误：用 `details: [{ field: "title", message: "..." }]` 数组带回，前端塞到 antd Form 的 `errors`。

---

## 3. 详情页路由（菜单补丁）

### 3.1 现有菜单的缺口

| 页面 | 路径 | 当前状态 |
|---|---|---|
| 工单列表 | `/ticket/list` | ✅ 已有（`V2026_05_19_02__add_ticket_module_menu.sql`）|
| 我的工单 | `/ticket/my` | ✅ 已有 |
| 工单分类 | `/ticket/category` | ✅ 已有 |
| **工单详情** | **`/ticket/detail/:id`** | ❌ 缺失（必补，否则前端列表的「查看」按钮无地方跳）|

### 3.2 补丁 SQL（新增）

文件名：`sql/mysql/migration/V2026_05_19_03__add_ticket_detail_menu.sql`

要点：

- `type=2` 菜单，但 `visible=b'0'`（侧栏不显示），让路由能匹配但导航不出现。
- `permission='ticket:ticket:query'` —— 复用列表的查询权限。
- `parent_id=@ticket_top`（挂在「工单中心」目录下）。
- 同样自动把权限授给 `role_id=1`（超管）。

详细 SQL 在 Step C 批 1 落地，本文档先约定字段：

```
name='工单详情'
permission='ticket:ticket:query'
type=2, visible=b'0', sort=99
path='detail/:id'
component='ticket/detail/index'
component_name='TicketDetail'
parent_id=@ticket_top
```

---

## 4. 权限矩阵 + 数据权限实现

### 4.1 角色定义（沿用 system 模块的角色体系）

| 角色 code | 名称 | 数据权限范围 |
|---|---|---|
| `super_admin` | 超级管理员（role_id=1） | 全部 |
| `ticket_admin` | 工单管理员（需新增） | 全部 |
| `dept_leader` | 部门负责人（已存在）| 本部门 + 下属部门 |
| `staff` | 普通员工（已存在）| 仅本人 |

> 一期暂不强制新增 `ticket_admin` 角色 —— 直接复用 `super_admin`；二期再补。

### 4.2 操作 × 角色 权限矩阵

| 操作 | 提单人本人 | 当前处理人 | 部门负责人 | 工单管理员 | 超管 |
|---|---|---|---|---|---|
| **查询列表** | 自己提的+处理的 | 自己处理的 | 本部门+下属部门 | 全部 | 全部 |
| **查询详情** | 自己提的+处理的 | 自己处理的 | 本部门+下属部门 | 全部 | 全部 |
| **创建** | ✓ | ✓ | ✓ | ✓ | ✓ |
| **修改基本信息** | 自己提的&状态=0 | ✗ | 本部门 | ✓ | ✓ |
| **删除** | ✗ | ✗ | ✗ | ✓ | ✓ |
| **分派** | ✗ | ✗ | 本部门 | ✓ | ✓ |
| **接单/开始** | ✗ | ✓ (自己) | ✗ | ✓ | ✓ |
| **完成** | ✗ | ✓ (自己) | ✗ | ✓ | ✓ |
| **关闭** | ✓ (自己提的&状态=3) | ✗ | ✗ | ✓ | ✓ |
| **重开** | ✗ | ✗ | ✗ | ✓ | ✓ |
| **转交** | ✗ | ✓ (自己) | 本部门 | ✓ | ✓ |
| **取消（待处理）** | ✓ (自己提的) | ✗ | ✗ | ✓ | ✓ |
| **评论** | ✓ (自己提的) | ✓ (自己处理) | 本部门 | ✓ | ✓ |
| **查看内部评论** | ✗ | ✓ | ✓ | ✓ | ✓ |

### 4.3 三层权限的代码落点

| 层级 | 实现方式 | 落点 |
|---|---|---|
| **菜单 / 按钮** | `system_role_menu` 关联 + 前端按 `permission` 字段过滤 | Step C 批 5：Controller 上 `@PreAuthorize("@ss.hasPermission('ticket:ticket:xxx')")` |
| **数据权限（部门维度）** | `DeptDataPermissionRule` 自动 SQL 重写 | Step C 批 5：Controller / Service 方法上加 `@DataPermission(includeRules = {DeptDataPermissionRule.class})`，按 `dept_id` 自动过滤 |
| **资源所有权（IDOR 防护）** | Service 层 `checkOwnership(ticketId, currentUserId)` 显式校验 | Step C 批 3-4：所有状态变更前必调；逻辑：`(creator_id == currentUserId) OR (assignee_id == currentUserId) OR isAdmin()`，否则抛 `TICKET_NO_PERMISSION` |

### 4.4 数据权限字段的对接细节

工单主表有两个跟"人"相关的字段：

- `dept_id` —— 工单归属部门（数据权限自动用此过滤）
- `assignee_id` —— 当前处理人（用于「我的工单」过滤，**不**走 DataPermission，而是手写 WHERE）

「我的工单」列表的查询条件：

```sql
SELECT * FROM shuhe_ticket
WHERE deleted = b'0'
  AND tenant_id = #{tenantId}
  AND (creator_id = #{userId} OR assignee_id = #{userId})
  -- 注意：此查询禁用 DataPermission，避免与部门规则双重过滤
ORDER BY create_time DESC
```

实现方式：在「我的工单」专用 Service 方法上加 `@DataPermission(enable = false)`，绕过部门规则。

### 4.5 后端 IDOR 校验模板（Step C 批 4 落地）

```java
private void checkTicketAccess(Long ticketId, Long currentUserId) {
    TicketDO ticket = ticketMapper.selectById(ticketId);
    if (ticket == null) throw exception(TICKET_NOT_EXISTS);
    if (SecurityFrameworkUtils.isSuperAdmin()) return;
    if (Objects.equals(ticket.getCreatorId(), currentUserId)) return;
    if (Objects.equals(ticket.getAssigneeId(), currentUserId)) return;
    // 部门负责人 / 工单管理员 走数据权限拦截，到这里说明都不是
    throw exception(TICKET_NO_PERMISSION);
}
```

---

## 5. 验证清单（Step A 完成的判定标准）

- [x] 状态有 6 个，转换矩阵覆盖 36 种 from→to 组合
- [x] action 有 11 个，每一个都列出 from/to/操作人
- [x] 错误码占段 `1_032_xxx_xxx`，与现有 10 个模块不冲突
- [x] 一期错误码 23 个，含主表 / 分类 / 状态机 / 评论 / 附件 / 权限 6 类
- [x] 详情页菜单字段写清楚（path / component / visible / permission）
- [x] 权限矩阵覆盖 14 种操作 × 5 种角色 = 70 个交叉点
- [x] 数据权限三层（菜单按钮 / 部门数据 / 资源所有权）都给出代码落点

---

## 6. 风险与待办

| 风险 | 缓解 |
|---|---|
| 「申请取消」「待审核」流程一期不做，二期补时可能要改状态机 | 设计文档已把这两个 action 列入二期范围，状态机已预留 `2 PENDING_REVIEW` 状态 |
| `assignee_id` 与数据权限规则的耦合方式仅在文档中描述，未实测 | Step C 批 5 联调时用 SQL 跟踪日志验证；如发现规则冲突，回退到「禁用 DataPermission + 手写 WHERE」方案 |
| 工单超时自动关闭依赖定时任务（quartz）| 一期先不做，列入二期 backlog；可手动 / 由管理员关闭 |
| 钉钉通知未做 | 一期先做站内信（沿用 system_notify 模块）；钉钉二期 |

---

## 7. 下一步

- [ ] **Step A 完成** → 本文档定稿
- [ ] **Step B：API 契约** → `docs/design/ticket-api.md`（11 个端点的 Req/Res schema + 错误码映射）
- [ ] **Step C 批 1**：enums + errorcode + 公用 DTO + 详情页菜单补丁 SQL
