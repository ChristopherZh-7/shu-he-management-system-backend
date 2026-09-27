# GolishAI 内部对接

本分支 `codex/golish-internal-integration` 对接 GolishAI 的 `codex/internal-platform-integration`。前端使用同名对接分支。

## 部署

1. 先执行 `sql/mysql/migration/V2026_09_27_01__golish_integration.sql`，创建不可变授权文件和工单投递表。原件及审批快照纳入备份。
2. 配置环境变量：`SHUHE_GOLISH_ENABLED=true`、`SHUHE_GOLISH_BASE_URL`（服务源地址）、`SHUHE_GOLISH_CLIENT_ID=shuhe`、`SHUHE_GOLISH_TOKEN`（独立密钥，至少 32 字节）。前端不配置或持有该密钥。
3. 在 GolishAI 启用 `server.internal_platform` 及 `execution`，配置来源租户、相同密钥、可用模型、轮次预算和测试限时，并启用现有 Harness。
4. 部署前端对接分支。在服务派遣工单中选择 `penetration_test` 服务项，勾选自动测试，输入网站与授权时间，上传原件。

集成默认关闭；未勾选自动测试的工单沿用现有流程。主管接单页展示授权资料，必须明确勾选批准。原有执行人字段作为本轮业务负责人的记录保留，开测由 GolishAI 自动处理。

## 数据与交付保证

- 审批人由已鉴权的主管接单流程提供，浏览器不能提供任意 `approved` 身份。审批页面快照与数据库不一致时拒绝审批；审批、修改、取消及删除先锁定工单行，再读取状态和快照，避免同秒修改绕过审批核对。
- 客户、项目和服务项从业务数据库读取，申请人从工单原记录读取。文件必须属于原申请人，审批展示的文件元数据须与归档原件一致。下载同时检查文件所有者、工单引用及工单访问权；自行填写别人的文件编号不会获得下载权限。
- 接单事务同时保存固定 `ticket-{id}:round-{roundId}` 编号及不可变 JSON；回滚不产生投递。后端不在接单事务内访问 GolishAI。
- 后台每 5 秒领取一份持久投递；数据库租约防止多实例同时发送。失联或部分上传失败后重发原 JSON 和原文件；GolishAI 依据编号、摘要和项目创建回执去重。
- 原件全部上传后幂等调用 `/start`，兼容此前只接收资料的 `prepared` 工单。后续每 15 秒查询执行状态。报告状态仍为 `not_generated`。
- 文件最大 8 MiB，最多 10 份；网站最多 50 个。当前只接受完整 HTTP(S) 网站授权，包括全部路径。账号密码不在本次合同中。
- 网络、429、5xx 自动退避重试。其他 4xx 标记为 `blocked`；原审批人处理配置或映射后重试，审批快照保持不变。改变目标或授权需重新申请工单。

## 权限与页面 API

所有接口位于已有管理 API 下，继续要求登录及工单权限：

| 路径 | 用途 |
| --- | --- |
| `GET /project/golish/capabilities` | 前端判断是否启用 |
| `POST /project/golish/documents` | 上传原始授权文件，服务端计算 SHA256 |
| `GET /project/golish/documents/{id}?ticketId=...` | 申请人或有权查看该工单的人下载对应原件 |
| `GET /project/golish/tickets/{ticketId}` | 查询本工单投递和执行进度 |
| `POST /project/golish/tickets/{ticketId}/retry` | 原审批人重试被阻断的同步 |

上传不是审批；仅普通工单状态变更也不会隐式触发测试。当前接入 `service_launch` 的主管接单审批，旧 BPM 直发申请尚未接入。测试完成后仍需后续报告生成、下载及业务验收对接。

已审批自动测试工单保留授权关联，禁止直接删除。当前工单状态机只允许待处理/已退回状态取消；运行期间可在 GolishAI 暂停，授权到期由 GolishAI 阻断工具调用并暂停项目。

## 本地验证

```text
mvn -pl shuhe-module-project -am test -Dtest=GolishIntegrationTest,TicketServiceImplTest#acceptAutomaticTicketRejectsStaleOrUnconfirmedApprovalBeforeWriting -Dsurefire.failIfNoSpecifiedTests=false
```

测试使用 H2 及本机模拟 HTTP 服务，验证审批事务回滚、可信身份、文件所有者、目标边界、固定请求重试、部分上传恢复和租约接管。没有连接生产数据库或扫描目标。

另有可选跨仓库用例：先在 GolishAI 内部分支设置 `GOLISH_INTEROP_READY_FILE` 为一个尚不存在的临时 JSON 文件路径，启动 `go test -short ./pkg/server/handlers -run '^TestInternalPlatformShuheInterop$' -count=1 -v`；该用例启动临时 HTTP 服务并写入连接信息。随后在本仓库执行上面的 Maven 命令，并追加 `-Dgolish.interop.readyFile=<同一路径>`。Go 用例最多等待五分钟。每次使用新路径，避免读取旧的完成标记。

该用例实际调用两边的审批快照生成、推送、原件归档、幂等开测及进度接口；仅 AI 执行引擎使用模拟服务，不访问授权目标。没有设置参数时自动跳过。

Windows 若 Microsoft JDK 的本机 HTTP 测试报 Unix domain socket `Invalid argument`，可仅给该次 Maven 测试添加 `-DargLine=-Djdk.net.unixdomain.tmpdir=<不存在的临时目录>`，使 JDK 的本机 selector 管道回退至 TCP；不修改系统设置。

基线 `ca61aa3` 的 `TicketServiceImplTest` 有 7 个既有失败（部门负责人/执行人测试和一个 Mockito 多余桩），在独立未修改工作区复现。本次专项测试不将这些既有失败计为通过。
