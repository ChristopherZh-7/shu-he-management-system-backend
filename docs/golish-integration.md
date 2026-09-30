# Golish 自动测试与修复复测

工单系统保留项目、资产输入、部门审批、整改和验收；Golish 无头服务保留执行授权、原生 Harness 会话、证据、漏洞和报告。两个服务不共用数据库，通过固定地址的鉴权 HTTP API 通信。

## 使用流程

1. 项目下添加并开始对应服务项。创建“服务派遣”工单，启用自动安全测试，填写站点名称、完整根地址、测试要求和时长。
2. 部门负责人接单并指定执行人，审批事务同时保存轮次、目标及不可变任务请求。提交后尚未审批不会扫描；后台工作器在事务提交后发送任务。
3. 初测结束后归档 JSON、Word，把漏洞关联到对应项目轮次。**工单保持处理中**，报告不能直接作为整改完成依据。
4. 整改后，部门负责人选择漏洞、填写整改说明和上线时间，确认原漏洞并批准复测。每轮复测都有独立审批、任务、原生复测会话、新证据和报告；可分批、多轮复测。
5. 所有初测漏洞的最新复测都为已修复，且初测没有未完成的覆盖项，才自动进入“待验收”。提单人验收通过、确认关闭沿用原有工单流程。未修复、无法确认或同步失败均不自动结束。

当前范围只接受 1–50 个明确 HTTP/HTTPS 站点根地址，不支持路径范围、通配符、CIDR 或排除清单。服务端拒绝这些输入，不会扩大成整站扫描。复测必须保持初测批准的目标编号和地址，且只能选择该工单初测报告内的漏洞。

## 部署配置

1. 部署 Golish `codex/headless-service` 分支，按其 `docs/headless-service.md` 配置组织、原生 Harness、模型、工具和报告渲染器。集成密钥只授权该组织。
2. 在工单数据库应用 `sql/mysql/migration/V2026_09_30_01__golish_integration.sql`。新增任务发件箱、回调去重表及漏洞对应表；已有轮次、目标和漏洞表转为 utf8mb4，避免证据文本中的 Unicode 字符导致归档失败。上线前按已有数据库变更流程备份并评估锁表时间。
3. 参考 `scripts/golish-integration/application-example.yaml`，通过环境变量注入 API 密钥、HMAC 密钥和私有报告目录。不要把密钥写入 Git、前端或工单扩展字段。
4. Golish 客户端配置固定回调地址 `https://工单域名/admin-api/project/golish/callback`，密钥与工单侧相同。生产使用 HTTPS；仅 loopback 联调允许 HTTP。
5. 编译部署后端和前端：Java 17 `mvn -B -DskipTests -Dmaven.jar.forceCreation=true package`；Node/pnpm 按仓库锁定版本执行 `pnpm install --frozen-lockfile`、`pnpm build:antd`。强制重建 jar 可避免增量打包沿用旧的 Spring Boot 嵌套依赖；也可使用 `clean package`。反向代理 `/admin-api` 到后端；报告目录不能映射为公开静态资源。

回调校验原始请求体、时间戳与 HMAC SHA256，容许五分钟时差，事件编号去重。它只唤醒状态查询，不直接覆盖任务状态，防止重复、乱序回调倒退状态。无效签名返回 HTTP 401，存储故障返回非 2xx，通知可重试；即使通知丢失，周期轮询仍会归档结果。

任务请求及幂等键持久化，提交响应丢失或重启不会新建扫描。工作器用数据库租约领取任务，并对整个 HTTP 响应施加超时与大小限制；状态、结果和报告仅从固定 Golish 服务地址读取。归档先写私有文件，再在单一事务中更新漏洞与工单。初测报告不可覆写；后续复测保留独立记录。

工单接口：`GET /project/golish/status?ticketId=`、`POST /project/golish/retest?ticketId=`、`POST /project/golish/retry?id=`、`GET /project/golish/report?id=`（均加 `/admin-api`）。查询/下载复用工单访问权限；批准复测或恢复执行还校验实际部门负责人或管理员。不能通过手动“提交完成”或直接调用验收接口绕过复测条件。

## 本机隔离验收

本次环境使用独立 MySQL 13316、Redis 16389、工单 API 48086、Golish API 8116、Harness 3086、前端 5668 和合成站点 18446，全部仅绑定回环地址。不要接入生产数据或启用钉钉通知。运行配置、测试账号密码、日志、报告和 PID 放在仓库外的 `.runtime/`，不提交 Git。

本机使用合成测试账号时，以 `VITE_LOCAL_PASSWORD_LOGIN=true pnpm build:antd` 构建前端，后端隔离配置启用 `shuhe.security.password-login-enabled`。该前端开关只在 loopback 地址显示密码表单，默认生产构建仍按原有钉钉登录策略。启动已构建前端：

```sh
python3 scripts/golish-integration/local-web.py --dist ../frontend/apps/web-antd/dist
```

管理已配置的本机服务（脚本只操作所属 PID，不初始化或删除数据库）：

```sh
python3 scripts/golish-integration/local-services.py status shuhe-backend --runtime ../.runtime
python3 scripts/golish-integration/local-services.py restart shuhe-backend --runtime ../.runtime
python3 scripts/golish-integration/local-services.py start golish-api --runtime ../.runtime --golish-repo /path/to/GolishAI
```

合成站点：`fixture-target.py --repair-flag ../.runtime/fixture-repaired`。标记文件不存在时，公开 `/debug/config` 返回明确标记为合成数据的配置；创建标记文件后返回 403，对所有响应增加安全头，并统一 Server 标识和错误响应，正常根页面和健康接口仍可访问。它没有真实凭据、数据库或出站访问。修复前后分别执行真正的模型测试和原生修复复测，不能向结果表填入模拟“通过”。

验收应检查：审批前没有任务、审批后只产生一个任务；初测报告可下载且工单保持处理中；手动完成被拒绝；修复后复测保存新证据；全部修复才进入待验收；人工验收和关闭可继续进行。生产部署、跨主机网络、真实客户资产、大规模并发和灾备恢复需另行验收。

### 2026-09-30 本机实测记录

工单 `TK20260930001` 对上述合成站点完成了一次真实初测和两轮原生复测。初测报告包含 3 项漏洞，工单保持处理中；第一轮复测确认 2 项已修复、1 项仍存在（默认错误页仍有实现特征），提前完成接口继续拒绝。补齐错误处理后，通过工单页面仅批准剩余 1 项的第二轮复测；结果为已修复，工单自动进入待验收。随后在页面人工验收、确认关闭，状态依次为 `1 → 2 → 3 → 4`，并非收到报告即自动关闭。

三次执行均保存了独立的报告与证据。初测重复审批被拒绝，相同复测请求重放返回同一任务；无签名回调返回 HTTP 401。任务执行期间工单后端重启后可继续归档，未重复创建扫描。隔离运行目录的 `e2e-evidence.json` 保存对应任务编号、阶段状态和报告摘要。仅验收本机合成站点，不代表真实客户资产、全部扫描工具或生产环境已经验收。

对应 Go 包测试、58 项 Java 测试及前端专项测试通过，Java 打包和前端生产构建通过。前端全量类型检查仍有上游既存错误，和同版本基线对比未新增错误，不能把该检查记作全部通过。
