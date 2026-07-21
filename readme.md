# Server — 棋牌游戏服务端

Java 8 / Maven 多模块 / Netty + Protobuf

## 项目结构

```
Server/
  utils/     # 运行时框架：net / threadtutil / config / metrics / trace / http
  tool/      # 工具 + 杂物（music/nginx/旧 db.mysql·redis / excel 等）
  proto/     # 协议与消息号
  lobby/     # 大厅（原 hall+room）+ SQLite 账号/邀请
  gate / center / game / web / robot / ...
```

utils 已合入本仓，不再依赖外仓 `com.cloud:utils` 远端仓库。

---

## 服务模块

### center — 注册中心

入口：`center/Center.java`

- 服务注册 / 发现 / 断线通知
- HTTP `/divide` 分配 Gate

### gate — 网关

入口：`gate/Gate.java`

- TCP + WebSocket
- 路由：`LOBBY_TYPE → Lobby`，`GAME_TYPE → Game`
- 断线只通知 Lobby + Game

### lobby — 大厅（合并原 hall + room）

入口：`lobby.Lobby`，默认端口 `5700`

- 单一进程 / 单一 `serverId` / 一套 `UserManager`
- 登录成功本地填 `tables`，不再跨服查桌
- 进桌仍 RPC → game（`SMsg.REQ_CREATE_TABLE_MSG`）
- SQLite：`data/lobby.db`
  - `user(id, username, nickname, password_hash, enabled, token, created_at, last_login_at)`
  - `invite(...)`
- 默认 `lobby.open-register=false`（需邀请码注册）
- 首次启动种子：`admin` / `admin123`，并创建一条 7 天/10 次邀请码（日志打印）

### game — 游戏服

入口：`game/Game.java`（玩法/状态机保持独立，未与 lobby 合并）

### web — Web 网关

入口：`web/Application.java`

- `POST /api/auth/login`、`POST /api/auth/register`、`GET /api/auth/registration`
- 房间：`GET /api/rooms`、`POST /api/rooms/join`
- WebSocket `/ws/game` 进游戏；token 以 Lobby 为准

### robot / sp / mcp / tool / proto

见各模块；协议消息类：`LMsg`(Lobby)、`GMsg`(Game)、`CMsg`、`SMsg`。

---

## 请求流程

```
浏览器 ──HTTP──► web ──TCP──► gate ──TCP──► lobby (登录/注册/房间列表/进桌)
浏览器 ──WS────► web ──TCP──► gate ──TCP──► game  (打牌)
```

## 协议要点（破坏性）

- `ServerType.Lobby`（原 Hall=3；已删 Room）
- `CMsg.LOBBY_TYPE=0x4000`；已删 `ROOM_TYPE`
- `lobby.proto`：`ReqLogin` / `AckLogin` / `ReqUserRegister` / `AckUserRegister` / 房间列表 / 进桌
- 已删 `ReqRoleRoomTable` 跨服链

## 构建

```bash
mvn install -DskipTests -pl '!mcp,!sp'
```

起服最短路径：`center + gate + lobby + game (+ web)`

## 明确不做

- game 与 lobby 合并
- 旧 Hall/Room 双进程兼容
- 外仓 utils 继续维护（内容已进本仓，远端仓库可删）
- Redis/MySQL 账号中台
