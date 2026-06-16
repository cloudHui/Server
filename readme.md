# Server — 棋牌游戏服务端

Java 8 / Maven 多模块 / Netty + Protobuf

## 项目结构

```
D:\code\st\
├── Server/          ← 本项目，游戏服务端
├── utils/           ← 底层框架库 (com.cloud:utils)
└── Database/        ← 旧版数据工具 (独立项目，未接入)
```

### 依赖关系

Server 所有模块依赖 `com.cloud:utils:1.0-SNAPSHOT`（位于 `D:\code\st\utils`），utils 提供：
- **网络层** — Netty TCP/WebSocket 服务器与客户端封装 (`net/`)
- **消息协议** — Protobuf SysMessage 封装、自定义 TCP 编解码、消息转发 (`net/message/`, `net/codec/`)
- **配置管理** — 从 `app.properties` 加载服务器地址与连接配置 (`utils/config/`)
- **数据库** — MySQL (Druid + iBatis) 和 Redis (Jedis) 访问层 (`db/`)，**Server 侧尚未接入**
- **线程池** — 支持按 groupId 串行化的执行器池，保证游戏逻辑线程安全 (`threadtutil/`)
- **定时器** — 延迟/间隔/有限次定时任务 (`threadtutil/timer/`)
- **监控** — 指标收集、链路追踪 (MDC)、CPU 告警、钉钉通知 (`utils/metrics/`, `monitor/`)
- **HTTP** — 基于 Netty 的 HTTP 服务器 (`http/`)
- **事件系统** — 条件匹配事件驱动 (`event/`)
- **工具类** — JSON、加密、时间、Excel 等 (`utils/other/`)

---

## 服务模块

### center — 注册中心

入口：`center/Center.java`

- 所有服务启动后主动注册到 Center，维护心跳
- 服务发现：Gate/Hall/Room/Game 启动时向 Center 查询其他服务地址并建立连接
- 服务断线通知：某服务断开时，Center 通知之前获取过该服务地址的相关服务
- HTTP 网关分配：`/divide` 接口根据客户端 IP 分配 Gate 地址（支持会话粘性）
- 客户端连接跟踪：维护 clientIp → gateAddr 映射，保证同一客户端打到同一 Gate

### gate — 网关服

入口：`gate/Gate.java`

- 双协议接入：同时启动 TCP 和 WebSocket 服务器
- 消息路由：根据消息 ID 的高位判断目标服务类型（Hall/Game/Room），转发请求
- 安全检查：未登录客户端只允许发送 login/register/heartbeat 消息
- 连接限流：WebSocket 路径下，同一 deviceId 10 秒内最多 3 次连接（滑动窗口）
- 异步响应分发：登录成功、加入桌子等异步响应回调到发起请求的客户端连接
- 消息拦截：游戏通知类消息（出牌、操作、结果）不转发给客户端，由 Game 直接处理

### hall — 大厅服

入口：`hall/Hall.java`

- 用户登录（3 条路径）：
  1. Token 重连 — 校验 token，找到已有用户，更新会话
  2. DeviceId 重连 — 同设备重连，复用用户
  3. 新用户注册 — AtomicInteger 自增 ID（从 1000 开始），创建内存用户
- Token 管理：UUID 生成，14 天过期，每小时清理，一人一 token
- 用户管理：双索引（userId + deviceId），最大 4096 在线
- 房间信息查询：登录后向 Room 查询用户所在的桌子/房间
- 俱乐部加入（Stub）：`ReqJoinClubHandler` 已注册但逻辑未实现

### game — 游戏服

入口：`game/Game.java`

- 线程池：默认 max(32, CPU核数)，队列容量 100000
- 桌子系统：抽象基类 `Table`，状态机驱动，支持多轮、回放录制、错误计数自动终止
- 状态机状态：WAITING → ROB/IDLE_ROB → CARD/IDLE_CARD → ROUND_OVER → TABLE_OVER，麻将有独立的 MJ_DEAL/MJ_PLAY/MJ_DISCARD/MJ_CLAIM 状态
- 桌子工厂：从 Excel 配置 `TableModel.xlsx` 创建，type=1 麻将，其他斗地主

#### 斗地主 (DDZ)

- 叫分/抢地主：两阶段 — 叫分阶段（1/2/3/不叫）+ 抢地主阶段（每次抢翻倍）
- 出牌逻辑：牌型分析 (`DdzRules.analyze`) + 大小比较 (`DdzRules.beats`)
- 结算：春天/反春天检测，倍数 = 基础分 × 抢地主倍数
- AI 系统：手牌拆解 (`DdzSplitPlanner`) + 合法出牌查找 (`DdzLegalBeatFinder`) + 局面观察 (`DdzVision`)
- 超时自动出牌：优先 AI 出牌，降级为最小牌或不出

#### 麻将 (MJ)

- 玩法变体：荆门麻将 (type=1，癞子 wildcard)、卡五星 (type=2，仅条/筒两门)
- 核心操作：摸牌、出牌、吃/碰/杠/胡检测（优先级：胡 > 杠 > 碰 > 吃）
- 杠牌：暗杠、补杠（含抢杠胡检测）、杠上开花
- 胡牌检查：基础检查器 + 荆门变体（癞子百搭）+ 卡五星变体
- 计分：按变体独立计分，番型计算 + 结算
- 回放：完整录制发牌、每步操作、最终状态、结算结果
- AI：`MjSimpleAi` + `MjVision`

### room — 房间服

入口：`room/Room.java`

- 房间管理：从 `TableModel.xlsx` 加载房间模板，维护 roomId → tableId → TableInfo
- 桌子状态：WAIT/PLAYING，`canJoin()` 判断：状态为 WAIT 且人数未满
- 匹配/建桌：玩家请求加入时，查找可加入的桌子或创建新桌，转发到 Game
- 用户跟踪：记录连接到本 Room 的用户及其桌子归属

### web — Web 网关

入口：`web/Application.java`（Spring Boot）

- 浏览器到游戏集群的桥接层
- HTTP REST：
  - `POST /api/login` — 传 nickname，走 Gate → Hall 完成登录，返回 sessionId + userId + token
  - `GET /api/validate?token=xxx` — token 校验
  - `POST /api/logout` — 清理会话和 Gate TCP 连接
  - `GET /api/rooms` — 获取房间列表
  - `POST /api/rooms/join` — 加入桌子
- WebSocket `/ws/game`：JSON 协议 `{"action":"xxx","seq":N,"data":{...}}`
  - `auth` — 绑定 WS 到已登录会话
  - `enterTable` — 进入桌子
  - `op` — 游戏操作（出牌、碰、杠等）
  - `leave` — 离开桌子
- 每个 Web 会话维护独立的 TCP 连接到 Gate，通过 CompletableFuture 实现异步请求-响应

### robot — AI 机器人

入口：`robot/Robot.java`

- 注册到 Center 作为 Robot 类型，获取 Gate 地址
- 有完整的响应处理器链（登录、进桌、建桌、加入房间）
- 两套 WebSocket 实现并存（Netty + Java-WebSocket），机器人编排逻辑尚未组装
- 状态：原型/开发中

### sp — 管理服务

入口：`sp/Application.java`（Spring Boot）

- `GET /up/ip` — 返回请求者 IP（NAT 检测）
- `GET /metrics` — JVM 内存 + CPU 核数
- `GET /health` — 健康检查
- 独立于游戏集群运行

### proto — 协议定义

- `.proto` 文件：`game.proto`, `gate.proto`, `hall.proto`, `room.proto`, `model.proto`, `const.proto`, `server.proto`
- Java 注解代码生成：`@ProcessType`, `@ProcessClass`, `@ClassField` 等注解驱动消息注册
- 消息类型枚举：`GMsg`(Game), `HMsg`(Hall), `RMsg`(Room), `CMsg`(Center), `SMsg`(Server)

### tool — 工具模块

辅助工具，非核心业务。

---

## 客户端请求流程

```
浏览器 ──HTTP──► web ──TCP──► gate ──TCP──► hall (登录，返回 token)
浏览器 ──WS────► web ──TCP──► gate ──TCP──► room (房间列表/加入)
浏览器 ──WS────► web ──TCP──► gate ──TCP──► game (游戏操作)
                                         │
                                         ▼
                                    广播回 gate → web → 浏览器
```

## 已完成

- [x] 多服务注册/发现 + 心跳 + 断线通知
- [x] Gate 消息路由 + 安全检查 + 连接限流
- [x] 用户登录（Token/DeviceId/新注册）+ Token 过期清理
- [x] 斗地主完整玩法（叫分、抢地主、出牌、春天检测、AI）
- [x] 麻将完整玩法（荆门、卡五星，吃碰杠胡、癞子、回放）
- [x] Web 网关（HTTP 登录 + WebSocket 游戏）
- [x] game/gate 支持多开
- [x] 一键部署脚本（bat 打包 → git 推送 → 远端拉取起服）
- [x] 远端配置/代码热更新 + 重启

## 待完成

### P0 — 阻塞级

- [ ] **数据库接入** — utils 已提供 DBService/Druid/Jedis，hall/db 下有 DAO/Entity/Mapper 但登录路径未调用，用户数据重启即丢
- [ ] **账号体系** — 当前只需 deviceId 即可登录，无密码/注册/第三方登录，proto 缺 loginType/loginKey 字段
- [ ] **userId 持久化** — AtomicInteger 自增从 1000 开始，重启归零，需接 DB 自增

### P1 — 高优先级

- [ ] **玩家系统** — 用户资料、背包、货币、好友、聊天
- [ ] **Room 重启房间恢复** — 服务器重启后房间状态丢失，玩家无法回到之前的桌子
- [ ] **大厅/Room 跨服处理** — 不在同一服务的两个玩家如何交互

### P2 — 功能完善

- [ ] **游戏规则完善** — 部分规则/番型可能不完整
- [ ] **客户端页面** — 前端 UI
- [ ] **行为树 AI** — 当前 AI 基于简单规则，可升级为行为树
- [ ] **俱乐部功能** — Handler 已注册但逻辑未实现
- [ ] **Robot 编排** — 机器人完整生命周期自动运行

### 已知问题

- 登录失败无错误响应，客户端等 3 秒超时
- Token 明文打印在 INFO 日志
- Web 模块 Token 与 Hall Token 不同步
- Token 通过 URL 参数传递（`/api?token=xxx`）
- TCP 路径无限流（仅 WebSocket 有限流）
- 限流器 `ConnectionRateLimiter` 有内存泄漏（不活跃设备永不清理）
- userId 类型不一致（DB: long，其他: int）
- addUser 并发缺陷（返回 void，幽灵用户风险）

## 构建与部署

```bash
# 构建全部模块
mvn clean package

# 一键部署（打包 + 推送到 git 仓库 + 远端拉取起服）
onekeyDeploy.bat
```

远端需安装 Git。支持通过脚本获取最新配置/代码并更新或重启服务。
