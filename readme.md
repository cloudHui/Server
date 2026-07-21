# Server — 棋牌游戏服务端

多人棋牌后端与简易 Web 前端。玩家可注册登录、邀请入局、选模板进桌（斗地主 / 麻将），完成对局流转、结算展示，并支持断线后拉回未结束的桌子。

技术栈：Java 8 · Maven 多模块 · Netty · Protobuf · Spring Boot（web）· SQLite（账号与邀请）

---

## 能做什么

| 能力 | 说明 |
|------|------|
| 账号 | 用户名 + 密码登录；Token 重连；默认需邀请码注册 |
| 邀请 | 管理员在后台创建 / 列表 / 复制链接 / 作废邀请码 |
| 大厅 | 按模板加入斗地主或麻将（规则在 Excel 模板表配置，客户端不填自定义规则） |
| 对局 | 进桌、操作、状态推送；小结算 / 总结算弹层 |
| 拉回 | 登录时若仍有未结束牌桌，列表自选进入，也可暂不进入去大厅 |
| 管理 | 种子账号 `admin` 登录后，大厅可见「邀请管理」页 |

---

## 仓库结构

```text
Server/
  utils/      运行时框架：网络、线程池、配置、指标、链路追踪、必要 HTTP
  tool/       工具与杂物（配置表、excel、旧封装等）
  proto/      协议定义与消息号
  center/     服务注册与发现
  gate/       对玩家网关（TCP / WebSocket），按消息类型转发 Lobby / Game
  lobby/      大厅：账号、邀请、房间列表、进桌编排 + SQLite
  game/       游戏服：玩法与状态机（独立进程，不与大厅合并）
  web/        HTTP + 静态页 + WebSocket，浏览器入口
  robot/      机器人（可选）
  mcp/ sp/    辅助模块（构建时可按需排除）
```

框架库已合入本仓 `utils/`，不再依赖外仓 utils。

---

## 运行时怎么串起来

```text
浏览器 ──HTTP──► web ──TCP──► gate ──► lobby   （登录 / 注册 / 房间 / 进桌）
浏览器 ──WS────► web ──TCP──► gate ──► game    （进桌后打牌与推送）
```

推荐起服顺序：`center` → `gate` → `lobby` → `game` → `web`。

典型玩家路径：

1. 打开 Web 登录页（可带 `/?invite=邀请码` 注册）
2. 登录成功；若有未结束桌子则弹层选择，或「暂不进入」进大厅
3. 大厅选斗地主 / 麻将模板加入（有空桌则入桌，否则由大厅向游戏服建新桌）
4. 牌桌页经 WebSocket 认证、进桌、操作；收到发牌 / 操作 / 结算推送
5. 管理员可在「邀请管理」生成邀请链接发给新用户

---

## 各模块说明

### center

服务注册、发现、断线通知。提供 HTTP 分配 Gate（如 `/divide`）。

### gate

玩家接入点。将大厅类消息转到 Lobby，游戏类消息转到 Game；玩家断开时通知 Lobby 与 Game（带连接标识，避免被顶号后的旧连接误伤新会话）。

默认玩家 TCP 端口：**5600**（Web 已对接该端口）。

### lobby

大厅业务单进程，默认端口 **5700**。

- SQLite 库：`data/lobby.db`（用户、邀请码）
- 配置 `lobby.open-register`：为 `false` 时注册必须带有效邀请码
- 邀请管理旁路 HTTP：本机 **5701**（仅 Web 反代，默认只听 `127.0.0.1`）
- 登录成功返回当前未结束桌子列表（含桌号、模板房间、玩法类型），供前端拉回
- 断线只标记离线，保留桌子归属，便于重登拉回；真正离桌走离开流程

首次启动若无用户，自动创建：

- 账号：`admin` / `admin123`
- 一条可用邀请码（启动日志打印；也可用管理页重新创建）

### game

斗地主、麻将等玩法与桌子状态机。对局中断线标记离线，重进同一桌可续上；大厅负责「有没有桌要拉回」，游戏服负责「桌上状态」。

房间规则来自配置表（如 `TableModel`）：`type=1` 麻将，`type=2` 斗地主。

### web

Spring Boot，默认 HTTP **8081**。

| 页面 | 用途 |
|------|------|
| `/` `index.html` | 登录 / 注册；可继续已登录会话或换号；有桌子时选桌 |
| `/room.html` | 大厅，按玩法进桌；管理员可见邀请入口 |
| `/doudizhu.html` | 斗地主牌桌 |
| `/mahjong.html` | 麻将牌桌 |
| `/admin.html` | 邀请码管理（仅 admin） |

主要接口：

- `POST /api/auth/login`、`POST /api/auth/register`、`GET /api/auth/registration`
- `GET /api/rooms`、`POST /api/rooms/join`
- `GET|POST /api/admin/invites*`（反代 lobby 邀请管理）
- WebSocket `/ws/game`：认证、进桌、操作；服务端转发游戏推送与结算

配置要点（`web/.../application.yml`）：

```yaml
gate:
  host: 127.0.0.1
  port: 5600
lobby:
  admin-http: http://127.0.0.1:5701
```

---

## 构建与启动

```bash
# 常用：跳过 mcp / sp
mvn install -DskipTests -pl '!mcp,!sp'

# 仅编译相关模块示例
mvn -pl proto,lobby,gate,game,web -am compile -DskipTests
```

各模块入口类见模块内 `*Application` / `Center` / `Gate` / `Lobby` / `Game`；配置在对应 `app.properties` 或 `application.yml`。

起服最短路径：`center + gate + lobby + game + web`，浏览器访问 `http://127.0.0.1:8081/`。

---

## 验收建议

1. `admin` / `admin123` 登录 → 大厅出现「邀请管理」→ 创建邀请码并复制链接  
2. 无痕窗口打开 `/?invite=码` 注册新号 → 进大厅 → 加入斗地主或麻将  
3. 对局中刷新登录页 →「继续进入」→ 应看到桌子列表（含玩法）→ 能回到牌桌  
4. 同账号再次登录后，旧连接断开不应把新会话打成离线  

---

## 已知后续（非阻塞）

- 邀请管理 HTTP 仍为轻量旁路；分机部署需改 `lobby.admin-http`；长期可改走大厅正式协议  
- Lobby 进程重启后内存中的「我的桌子」会丢，可再补「向 game 查询所在桌」  
- 麻将前端吃碰杠胡等操作仍需补齐；生产环境请修改默认管理员密码  
- 冗余静态页 `game.html`、旧登录路径可逐步清理  

更细的问题跟踪见仓库外工作笔记时，以本 `readme` 的产品与运维描述为准。
