package com.gamer.data.mpcserver;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gamer.data.mpcserver.commands.CommandHandler;
import com.gamer.data.mpcserver.core.CommandContext;
import com.gamer.data.mpcserver.core.CommandDispatcher;
import com.gamer.data.mpcserver.core.CommandResult;
import com.gamer.data.mpcserver.core.DbDefaults;
import com.gamer.data.mpcserver.core.FileSandbox;
import com.gamer.data.mpcserver.core.GitDefaults;
import com.gamer.data.mpcserver.core.McpUtils;
import com.gamer.data.mpcserver.core.RedisDefaults;
import com.gamer.data.mpcserver.log.DailyFileLog;
import com.gamer.data.mpcserver.protocol.McpRequest;
import com.gamer.data.mpcserver.protocol.McpResponse;

/**
 * MCP 服务端入口：stdio 单行 JSON 请求 → 执行命令 → 单行 JSON 响应。
 *
 * <p>
 * <b>交互约定</b>：输入一行一个 JSON（{@link McpRequest}），输出一行一个 JSON（{@link McpResponse}）； 协议错误返回 fail（如 PARSE_ERROR /
 * INVALID_REQUEST）。不引入网络层，仅靠标准输入输出与调用方集成。
 * </p>
 *
 * <p>
 * <b>结构概览</b>：
 * </p>
 * <ul>
 * <li>main：解析启动参数、注册命令、stdin 循环读行并写回响应，处理 exit/shutdown 状态</li>
 * <li>handleOne：解析 JSON → 提取 method/params/id → 先走 MCP 标准方法，再走自定义 method 分发</li>
 * <li>handleMcpMethods：按顺序尝试 legacy exit、shutdown、exit 通知、initialize、tools/list、tools/call 等</li>
 * <li>buildToolsListResult / callTool：MCP 工具发现与调用的实现</li>
 * </ul>
 *
 * @author liuyunhui
 * @date 2026-03-18
 * @version 1.0
 */
public class McpServer {
    /**
     * 默认日志目录名（沿用现有拼写；不要随意修改以免影响部署/脚本约定）。
     */
    private static final String DEFAULT_LOG_DIR = ".cursor/mcpsever";

    /**
     * MCP 协议版本字符串（出现在 initialize.result.protocolVersion）。
     *
     * <p>
     * 它不是“你服务端代码的版本号”，而是你宣称遵循的 MCP 协议版本标识。通常保持与 MCP 规范/SDK 推荐值一致即可；如需做版本协商，可按客户端传入 params.protocolVersion 做降级选择。
     * </p>
     */
    private static final String PROTOCOL_VERSION = "2025-11-25";

    // ===== JSON-RPC / MCP 错误码（集中管理，避免散落 magic number） =====
    private static final int ERR_PARSE_ERROR = -32700;// 解析错误
    private static final int ERR_INVALID_REQUEST = -32600;// 无效请求
    private static final int ERR_METHOD_NOT_FOUND = -32601;// 方法不存在
    private static final int ERR_INVALID_PARAMS = -32602;// 无效参数
    private static final int ERR_INTERNAL_ERROR = -32603;// 内部错误

    /** DB 启动参数前缀，与 parseDbDefaultsFromArgs 内 switch 下标一一对应。 */
    private static final String[] DB_ARG_PREFIXES =
        {"--dbHost=", "--dbPort=", "--dbUser=", "--dbPassword=", "--dbDatabase="};

    private static DailyFileLog log;

    /**
     * 通用启动入口：根据 profile 注册工具并运行 MCP stdio 循环。
     *
     * <p>
     * 三个服务（Excel/DB/FS）应复用本方法；profile 仅负责声明“暴露哪些工具”。
     * </p>
     */
    public static void run(ServerProfile profile, String[] args, String logDirName) throws Exception {
        if (profile == null) {
            profile = ServerProfile.ALL;
        }
        File workDir = new File(System.getProperty("user.dir"));
        File logDir = new File(workDir, logDirName == null ? DEFAULT_LOG_DIR : logDirName);
        log = new DailyFileLog(logDir);

        ObjectMapper mapper = new ObjectMapper();
        DbDefaults dbDefaults = parseDbDefaultsFromArgs(args);
        RedisDefaults redisDefaults = parseRedisDefaultsFromArgs(args);
        GitDefaults gitDefaults = parseGitDefaultsFromArgs(args);
        List<File> allowedDirs = parseAllowedDirsFromArgs(workDir, args);
        FileSandbox fileSandbox = new FileSandbox(workDir, allowedDirs);
        CommandContext ctx = new CommandContext(mapper, log, dbDefaults, fileSandbox, redisDefaults, gitDefaults);

        CommandDispatcher dispatcher = new CommandDispatcher();
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8));

        boolean shutdownRequested = false;
        String line;
        log.logMessage("[mcpserver] start profile=" + profile.id);
        while ((line = in.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty()) {
                continue;
            }

            log.logMessage("[mcpserver] request:" + line);
            HandleResult hr = handleOne(profile, dispatcher, ctx, line, shutdownRequested);
            McpResponse resp = hr == null ? null : hr.response;
            if (resp != null) {
                out.write(mapper.writeValueAsString(resp));
                out.newLine();
                out.flush();
            }

            log.logMessage("[mcpserver] response:" + resp);
            if (hr != null && hr.shouldExit) {
                log.logMessage("[mcpserver] exit by request:" + hr.response);
                break;
            }
            if (hr != null && hr.shutdownRequested) {
                shutdownRequested = true;
            }
        }

        log.logMessage("[mcpserver] stdin closed, exit");
        log.close();
    }

    /**
     * 从启动参数解析 DB 默认连接。
     *
     * <p>
     * 支持形式：--dbHost=、--dbPort=、--dbUser=、--dbPassword=、--dbDatabase=；未提供 user 则返回 null。
     * </p>
     */
    private static DbDefaults parseDbDefaultsFromArgs(String[] args) {
        if (args == null || args.length == 0) {
            return null;
        }
        DbDefaultsBuilder db = new DbDefaultsBuilder();
        for (String a : args) {
            if (a == null || a.trim().isEmpty()) {
                continue;
            }
            applyOneDbArg(db, a.trim());
        }
        if (db.user == null || db.user.trim().isEmpty()) {
            return null;
        }
        if (db.password == null) {
            db.password = "";
        }
        return new DbDefaults(db.host, db.port, db.user, db.password, db.database);
    }

    /**
     * 从启动参数解析 Redis 默认连接信息。
     *
     * <p>
     * 支持形式：
     * </p>
     * <ul>
     * <li>--redisHost=</li>
     * <li>--redisPort=</li>
     * <li>--redisUser=</li>
     * <li>--redisPassword=</li>
     * </ul>
     */
    private static RedisDefaults parseRedisDefaultsFromArgs(String[] args) {
        RedisDefaultsBuilder redis = new RedisDefaultsBuilder();
        if (args != null) {
            for (String a : args) {
                if (a == null || a.trim().isEmpty()) {
                    continue;
                }
                String s = a.trim();
                if (s.startsWith("--redisHost=")) {
                    redis.host = s.substring("--redisHost=".length());
                    continue;
                }
                if (s.startsWith("--redisPort=")) {
                    redis.port = tryParseInt(s.substring("--redisPort=".length()));
                    continue;
                }
                if (s.startsWith("--redisUser=")) {
                    redis.user = s.substring("--redisUser=".length());
                    continue;
                }
                if (s.startsWith("--redisPassword=")) {
                    redis.password = s.substring("--redisPassword=".length());
                }
            }
        }

        return new RedisDefaults(redis.host, redis.port, redis.user, redis.password);
    }

    /**
     * 从启动参数解析 Git 仓库路径：--gitRepo=PATH。
     */
    private static GitDefaults parseGitDefaultsFromArgs(String[] args) {
        if (args == null) {
            return null;
        }
        int i;
        for (i = 0; i < args.length; i++) {
            String a = args[i];
            if (a == null) {
                continue;
            }
            String s = a.trim();
            if (s.startsWith("--gitRepo=")) {
                String path = s.substring("--gitRepo=".length()).trim();
                if (path.isEmpty()) {
                    return null;
                }
                return new GitDefaults(new File(path));
            }
        }
        return null;
    }

    /**
     * 根据前缀把单个参数字符串应用到 DbDefaultsBuilder；匹配 DB_ARG_PREFIXES[i] 时按 i 写入对应字段。
     */
    private static void applyOneDbArg(DbDefaultsBuilder db, String s) {
        for (int i = 0; i < DB_ARG_PREFIXES.length; i++) {
            if (!s.startsWith(DB_ARG_PREFIXES[i])) {
                continue;
            }
            String val = s.substring(DB_ARG_PREFIXES[i].length());
            switch (i) {
                case 0:
                    db.host = val;
                    break;
                case 1:
                    db.port = tryParseInt(val);
                    break;
                case 2:
                    db.user = val;
                    break;
                case 3:
                    db.password = val;
                    break;
                case 4:
                    db.database = val;
                    break;
                default:
                    break;
            }
            return;
        }
    }

    /** 用于收集 DB 启动参数的临时对象，仅 parseDbDefaultsFromArgs 使用。 */
    private static class DbDefaultsBuilder {
        String host;
        Integer port;
        String user;
        String password;
        String database;
    }

    /** 用于收集 Redis 启动参数的临时对象，仅 parseRedisDefaultsFromArgs 使用。 */
    private static class RedisDefaultsBuilder {
        String host;
        Integer port;
        String user;
        String password;
    }

    /** 安全解析整数，失败或空串返回 null。 */
    private static Integer tryParseInt(String s) {
        if (s == null || s.trim().isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception ignored) {
            return null;
        }
    }

    /** 单参数类型：用于 parseAllowedDirsFromArgs 分支。 */
    private static final int FS_ARG_FS_ALLOW = 0;
    private static final int FS_ARG_DB = 1;
    private static final int FS_ARG_OTHER_OPTION = 2;
    private static final int FS_ARG_BARE = 3;

    /**
     * 解析允许访问的目录（文件系统沙箱根目录）。
     *
     * <p>
     * 支持 --fsAllow=PATH（可重复）、裸参数视为目录；--db* 与其它 --xxx 忽略。未指定时默认仅允许 workDir。
     * </p>
     */
    private static List<File> parseAllowedDirsFromArgs(File workDir, String[] args) {
        List<File> roots = new ArrayList<>();
        if (args != null) {
            for (String arg : args) {
                String s = arg == null ? null : arg.trim();
                if (s == null || s.isEmpty()) {
                    continue;
                }
                int kind = fsArgKind(s);
                if (kind == FS_ARG_FS_ALLOW) {
                    String p = s.substring("--fsAllow=".length()).trim();
                    if (!p.isEmpty()) {
                        roots.add(new File(p));
                    }
                    continue;
                }
                if (kind == FS_ARG_DB || kind == FS_ARG_OTHER_OPTION) {
                    continue;
                }
                roots.add(new File(s));
            }
        }
        if (roots.isEmpty()) {
            // 未显式指定时，按项目约定自动开放三个根目录：
            // - Server（当前工作区，对应游戏各工程）
            // - ../Document（设计文档与配置表所在目录）
            // - ../Common/Tools/Bin（工具与脚本目录）
            roots.add(workDir);
            File parent = workDir.getParentFile();
            if (parent != null) {
                File documentDir = new File(parent, "Document");
                roots.add(documentDir);
                File commonToolsBin = new File(new File(new File(parent, "Common"), "Tools"), "Bin");
                roots.add(commonToolsBin);
            }
        }
        try {
            log.logMessage("[mcpserver] fs.allowedRoots=" + roots.size());
            for (int i = 0; i < roots.size(); i++) {
                File r = roots.get(i);
                log.logMessage("[mcpserver] fs.root[" + i + "]=" + (r == null ? "null" : r.getPath()));
            }
        } catch (Exception ignored) {
        }
        return roots;
    }

    /** 判断单参数属于哪一类：fsAllow / db / 其它长选项 / 裸目录。 */
    private static int fsArgKind(String s) {
        if (s.startsWith("--fsAllow=")) {
            return FS_ARG_FS_ALLOW;
        }
        if (s.startsWith("--dbHost=") || s.startsWith("--dbPort=") || s.startsWith("--dbUser=")
            || s.startsWith("--dbPassword=") || s.startsWith("--dbDatabase=")) {
            return FS_ARG_DB;
        }
        if (s.startsWith("--")) {
            return FS_ARG_OTHER_OPTION;
        }
        return FS_ARG_BARE;
    }

    /**
     * 处理单行请求：解析 JSON → 校验 method/params/id → 先走 MCP 标准方法，再走自定义 method 或报错。
     *
     * @param dispatcher
     *            命令调度器
     * @param ctx
     *            命令上下文
     * @param line
     *            请求行（已 trim）
     * @param shutdownRequested
     *            是否已收到 shutdown
     * @return 处理结果；response 为 null 表示 notification 无需回包
     */
    private static HandleResult handleOne(ServerProfile profile, CommandDispatcher dispatcher, CommandContext ctx,
        String line, boolean shutdownRequested) {
        ObjectMapper mapper = ctx.mapper();
        JsonNode root;
        try {
            root = mapper.readTree(line);
        } catch (Exception e) {
            ctx.log().logMessage("[mcpserver] parseFail line:" + McpUtils.oneLine(line) + " err:" + e);
            return HandleResult.of(McpResponse.fail(null, ERR_PARSE_ERROR, "Parse error"), false, false);
        }

        if (root == null || !root.isObject()) {
            return HandleResult.of(McpResponse.fail(null, ERR_INVALID_REQUEST, "Invalid Request"), false, false);
        }

        ObjectNode obj = (ObjectNode)root;
        RequestParts p = RequestParts.parse(mapper, obj);
        String method = p.method;
        JsonNode paramsNode = p.params;
        JsonNode idNode = p.idNode;
        Object id = p.id;

        if (method == null || method.trim().isEmpty()) {
            if (idNode == null) {
                return HandleResult.none();
            }
            return HandleResult.of(McpResponse.fail(id, ERR_INVALID_REQUEST, "Invalid Request: method required"), false,
                false);
        }

        HandleResult mcp =
            handleMcpMethods(profile, dispatcher, ctx, method, idNode, id, paramsNode, shutdownRequested);
        if (mcp != null) {
            return mcp;
        }

        CommandHandler handler = dispatcher.get(method);
        if (handler == null) {
            return HandleResult.of(McpResponse.fail(id, ERR_METHOD_NOT_FOUND, "Method not found: " + method), false,
                false);
        }

        try {
            CommandResult r = handler.handle(ctx, paramsNode);
            String text = r == null ? "" : r.text;
            return HandleResult.of(McpResponse.ok(id, text), false, false);
        } catch (IllegalArgumentException e) {
            ctx.log().logMessage("[mcpserver] invalidParams method:" + method + " err:" + e.getMessage());
            return HandleResult.of(McpResponse.fail(id, ERR_INVALID_PARAMS, e.getMessage()), false, false);
        } catch (Exception e) {
            ctx.log().logMessage("[mcpserver] handlerFail method:" + method + " err:" + e);
            return HandleResult.of(McpResponse.fail(id, ERR_INTERNAL_ERROR, simplify(e)), false, false);
        }
    }

    /**
     * 处理 MCP 标准方法与历史兼容方法；按顺序尝试各子处理器，先匹配先返回。
     *
     * <p>
     * 顺序：兼容 exit/quit → shutdown → exit(notification) → initialize → notifications/initialized → tools/list →
     * tools/call；均不匹配时返回 null，由调用方走自定义 method 分支。
     * </p>
     */
    private static HandleResult handleMcpMethods(ServerProfile profile, CommandDispatcher dispatcher,
        CommandContext ctx, String method, JsonNode idNode, Object id, JsonNode paramsNode, boolean shutdownRequested) {
        HandleResult r;
        r = tryHandleLegacyExit(ctx, method, id);
        if (r != null) {
            return r;
        }
        r = tryHandleShutdown(ctx, method, idNode, id);
        if (r != null) {
            return r;
        }
        r = tryHandleExitNotification(ctx, method, idNode, shutdownRequested);
        if (r != null) {
            return r;
        }
        r = tryHandleInitialize(profile, method, idNode, id);
        if (r != null) {
            return r;
        }
        r = tryHandleInitialized(method);
        if (r != null) {
            return r;
        }
        r = tryHandleToolsList(dispatcher, method, idNode, id);
        if (r != null) {
            return r;
        }
        r = tryHandleToolsCall(dispatcher, ctx, method, idNode, id, paramsNode);
        return r;
    }

    /** 兼容旧控制命令：exit/quit 且为 request（带 id）时回包 "bye" 并标记退出。 */
    private static HandleResult tryHandleLegacyExit(CommandContext ctx, String method, Object id) {
        if (!"exit".equalsIgnoreCase(method) && !"quit".equalsIgnoreCase(method)) {
            return null;
        }
        ctx.log().logMessage("[mcpserver] legacy exit requested");
        return HandleResult.of(McpResponse.ok(id, "bye"), true, false);
    }

    /** shutdown：带 id 时回包并标记已请求关闭；notification 不回包。 */
    private static HandleResult tryHandleShutdown(CommandContext ctx, String method, JsonNode idNode, Object id) {
        if (!"shutdown".equalsIgnoreCase(method)) {
            return null;
        }
        if (idNode == null) {
            return HandleResult.none();
        }
        ctx.log().logMessage("[mcpserver] shutdown requested");
        return HandleResult.of(McpResponse.okResult(id, new HashMap<String, Object>()), false, true);
    }

    /** exit 作为 notification（无 id）时，仅在已 shutdown 后真正退出。 */
    private static HandleResult tryHandleExitNotification(CommandContext ctx, String method, JsonNode idNode,
        boolean shutdownRequested) {
        if (!"exit".equalsIgnoreCase(method) || idNode != null) {
            return null;
        }
        if (!shutdownRequested) {
            ctx.log().logMessage("[mcpserver] exit notification before shutdown, ignore");
            return HandleResult.none();
        }
        ctx.log().logMessage("[mcpserver] exit notification received");
        return HandleResult.of(null, true, false);
    }

    /** initialize：带 id 时回包协议版本与能力；notification 不回包。 */
    private static HandleResult tryHandleInitialize(ServerProfile profile, String method, JsonNode idNode, Object id) {
        if (!"initialize".equalsIgnoreCase(method)) {
            return null;
        }
        if (idNode == null) {
            return HandleResult.none();
        }
        return HandleResult.of(McpResponse.okResult(id, buildInitializeResult(profile)), false, false);
    }

    /** notifications/initialized：仅 notification，不回包。 */
    private static HandleResult tryHandleInitialized(String method) {
        if (!"notifications/initialized".equalsIgnoreCase(method)) {
            return null;
        }
        return HandleResult.none();
    }

    /** tools/list：带 id 时返回工具列表。 */
    private static HandleResult tryHandleToolsList(CommandDispatcher dispatcher, String method, JsonNode idNode,
        Object id) {
        if (!"tools/list".equalsIgnoreCase(method) || idNode == null) {
            return null;
        }
        return HandleResult.of(McpResponse.okResult(id, buildToolsListResult(dispatcher.methods())), false, false);
    }

    /** tools/call：带 id 时执行工具并回包 content。 */
    private static HandleResult tryHandleToolsCall(CommandDispatcher dispatcher, CommandContext ctx, String method,
        JsonNode idNode, Object id, JsonNode paramsNode) {
        if (!"tools/call".equalsIgnoreCase(method) || idNode == null) {
            return null;
        }
        return HandleResult.of(McpResponse.okResult(id, callTool(dispatcher, ctx, paramsNode)), false, false);
    }

    /** 从请求 JSON 解析出的 method / params / id；idNode 为 null 表示 notification（无 id 字段）。 */
    private static final class RequestParts {
        private final String method;
        private final JsonNode params;
        private final JsonNode idNode;
        private final Object id;

        private RequestParts(String method, JsonNode params, JsonNode idNode, Object id) {
            this.method = method;
            this.params = params;
            this.idNode = idNode;
            this.id = id;
        }

        private static RequestParts parse(ObjectMapper mapper, ObjectNode obj) {
            JsonNode methodNode = obj.get("method");
            String method = methodNode == null || methodNode.isNull() ? null : methodNode.asText();
            JsonNode paramsNode = obj.get("params");
            JsonNode idNode = obj.get("id");
            Object id = idNode == null || idNode.isNull() ? null : mapper.convertValue(idNode, Object.class);
            return new RequestParts(method, paramsNode, idNode, id);
        }
    }

    /** 单次处理结果：response 为 null 表示不回包；shouldExit 为 true 时主循环退出；shutdownRequested 记录已收 shutdown。 */
    private static class HandleResult {
        private final McpResponse response;// 响应
        private final boolean shouldExit;// 是否退出
        private final boolean shutdownRequested;// 是否请求关闭

        private HandleResult(McpResponse response, boolean shouldExit, boolean shutdownRequested) {
            this.response = response;
            this.shouldExit = shouldExit;
            this.shutdownRequested = shutdownRequested;
        }

        private static HandleResult of(McpResponse response, boolean shouldExit, boolean shutdownRequested) {
            return new HandleResult(response, shouldExit, shutdownRequested);
        }

        private static HandleResult none() {
            return new HandleResult(null, false, false);
        }
    }

    /**
     * 将异常对象简化为可返回给调用方的短文本：优先 message，其次类名。
     */
    private static String simplify(Exception e) {
        if (e == null) {
            return "";
        }
        String msg = e.getMessage();
        if (msg != null && !msg.trim().isEmpty()) {
            return msg;
        }
        return e.getClass().getName();
    }

    /** 构建 MCP initialize 的 result：protocolVersion、capabilities（tools）、serverInfo、instructions。 */
    private static Map<String, Object> buildInitializeResult(ServerProfile profile) {
        if (profile == null) {
            profile = ServerProfile.ALL;
        }
        Map<String, Object> caps = new HashMap<>();
        caps.put("tools", mapOf());

        Map<String, Object> serverInfo = new HashMap<>();
        serverInfo.put("name", profile.serverName);
        serverInfo.put("title", profile.serverTitle);
        serverInfo.put("version", "1.0.0");
        serverInfo.put("description", profile.serverDescription);

        Map<String, Object> result = new HashMap<>();
        result.put("protocolVersion", PROTOCOL_VERSION);
        result.put("capabilities", caps);
        result.put("serverInfo", serverInfo);
        result.put("instructions", profile.instructions);
        return result;
    }

    /**
     * 服务配置（profile）：用于在同一套 MCP 协议实现上拆分不同的“工具集合”。
     */
    public static final class ServerProfile {
        public static final ServerProfile ALL = new ServerProfile("all", "my-java-tool", "My Java MCP Server",
            "Java stdio MCP server for KingdomWarships tooling",
            "Use tools/list then tools/call. This server supports command-doc tool names for excel/mysql/filesystem/redis.");

        public static final ServerProfile EXCEL =
            new ServerProfile("excel", "mcp-excel-tool", "Excel MCP Server", "Excel tools for KingdomWarships tooling",
                "Use tools/list then tools/call. This server supports excel_* tools.");

        public static final ServerProfile DB = new ServerProfile("db", "mcp-db-tool", "DB MCP Server",
            "Database tools (local_*) for KingdomWarships tooling",
            "Use tools/list then tools/call. This server supports: help, local_get_database_info, local_sql_query, local_check_permissions, local_get_ddl_sql_logs, local_get_operation_logs.");

        public static final ServerProfile FS = new ServerProfile("fs", "mcp-fs-tool", "Filesystem MCP Server",
            "Filesystem tools (fs.*) for KingdomWarships tooling",
            "Use tools/list then tools/call. This server supports command-doc filesystem tools.");

        public static final ServerProfile REDIS = new ServerProfile("redis", "mcp-redis-tool", "Redis MCP Server",
            "Redis tools (key/value helpers) for KingdomWarships tooling",
            "Use tools/list then tools/call. This server supports redis get/set/delete/list.");

        public static final ServerProfile GIT = new ServerProfile("git", "mcp-git-tool", "Git MCP Server",
            "Read-only Git query tools for KingdomWarships work summaries",
            "Use tools/list then tools/call. Read-only: git_log, git_show, git_commit_files, git_run (whitelist). "
                + "Configure --gitRepo= in mcp.json. Filter by author in tool params.");

        public static final ServerProfile IMAGE = new ServerProfile("image", "mcp-image-tool", "Image MCP Server",
            "Image recognition tools using MiMo v2.5 vision model",
            "Use tools/list then tools/call. This server supports describe_image and describe_image_base64. "
                + "Configure --mimoApiKey= in mcp.json.");

        public static final ServerProfile MIMO = new ServerProfile("mimo", "mcp-mimo-tool", "MiMo MCP Server",
            "MiMo token usage query tools",
            "Use tools/list then tools/call. This server supports mimo_usage to query token credits. "
                + "Configure --serviceToken= and --userId= in mcp.json.");

        private final String id;
        private final String serverName;
        private final String serverTitle;
        private final String serverDescription;
        private final String instructions;

        private ServerProfile(String id, String serverName, String serverTitle, String serverDescription,
            String instructions) {
            this.id = id;
            this.serverName = serverName;
            this.serverTitle = serverTitle;
            this.serverDescription = serverDescription;
            this.instructions = instructions;
        }
    }

    /**
     * 构建 MCP tools/list 的返回体：仅包含已注册且对外暴露的工具（不含 exit/quit 等控制类 method）。
     */
    private static Map<String, Object> buildToolsListResult(Set<String> methods) {
        List<Map<String, Object>> tools = new ArrayList<>();
        List<Map<String, Object>> all = ToolRegistry.getAllToolDefs();
        for (Map<String, Object> tool : all) {
            Object nameObj = tool.get("name");
            String name = nameObj == null ? null : String.valueOf(nameObj);
            if (name != null && methods.contains(name)) {
                tools.add(tool);
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put("tools", tools);

        return result;
    }

    /**
     * tools/list 静态定义仓库：在类加载时完成初始化，buildToolsListResult 仅做过滤。
     *
     * <p>
     * 约束：每份工具定义都独立成一个私有方法，便于按工具维度维护参数与文案。
     * </p>
     */
    private static final class ToolRegistry {
        private static final List<Map<String, Object>> ALL = buildAll();

        private ToolRegistry() {}

        private static List<Map<String, Object>> getAllToolDefs() {
            return ALL;
        }

        private static List<Map<String, Object>> buildAll() {
            List<Map<String, Object>> list = new ArrayList<>();
            list.add(helpTool());
            list.add(pingTool());
            list.add(excelDescribeSheetsTool());
            list.add(excelReadSheetTool());
            list.add(excelWriteToSheetTool());
            list.add(excelCopySheetTool());
            list.add(excelCreateTableTool());
            list.add(excelFormatRangeTool());
            list.add(excelScreenCaptureTool());
            list.add(localGetDatabaseInfoTool());
            list.add(localSqlQueryTool());
            list.add(localCheckPermissionsTool());
            list.add(localGetDdlSqlLogsTool());
            list.add(localGetOperationLogsTool());
            list.add(listAllowedDirectoriesTool());
            list.add(readTextFileTool());
            list.add(readMediaFileTool());
            list.add(readMultipleFilesTool());
            list.add(editFileTool());
            list.add(createDirectoryTool());
            list.add(listDirectoryTool());
            list.add(listDirectoryWithSizesTool());
            list.add(directoryTreeTool());
            list.add(moveFileTool());
            list.add(getFileInfoTool());
            list.add(searchFilesTool());
            list.add(redisGetTool());
            list.add(redisSetTool());
            list.add(redisDeleteTool());
            list.add(redisListTool());
            list.add(writeFileTool());
            list.add(gitLogTool());
            list.add(gitShowTool());
            list.add(gitCommitFilesTool());
            list.add(gitRunTool());
            list.add(describeImageTool());
            list.add(describeImageBase64Tool());
            list.add(mimoUsageTool());
            return list;
        }

        private static Map<String, Object> helpTool() {
            return toolDef("help", "Help", "列出可用命令与参数示例。", schemaObject(new HashMap<>(), new String[0]), annReadOnly());
        }

        private static Map<String, Object> pingTool() {
            return toolDef("ping", "Ping", "心跳检测：快速验证 MCP 服务是否可用。",
                schemaObject(new HashMap<>(), new String[0]), annReadOnly());
        }

        private static Map<String, Object> excelDescribeSheetsTool() {
            Map<String, Object> props = new HashMap<>();
            props.put("fileAbsolutePath", schemaString("Excel 文件绝对路径"));
            return toolDef("excel_describe_sheets", "Excel Describe Sheets", "列出 Excel 的工作表信息。",
                schemaObject(props, new String[] {"fileAbsolutePath"}), annReadOnly());
        }

        private static Map<String, Object> excelReadSheetTool() {
            Map<String, Object> props = new HashMap<>();
            props.put("fileAbsolutePath", schemaString("Excel 文件绝对路径"));
            props.put("sheetName", schemaString("Sheet 名称"));
            props.put("range", schemaString("读取范围（可选）"));
            props.put("showFormula", schemaString("是否显示公式（可选）"));
            props.put("showStyle", schemaString("是否显示样式（可选）"));
            // 兼容当前实现参数
            props.put("sheetIndex", schemaInt("Sheet 索引（可选，兼容）"));
            props.put("maxRows", schemaInt("最多读取行数（可选，兼容）"));
            props.put("maxCols", schemaInt("每行最多读取列数（可选，兼容）"));
            return toolDef("excel_read_sheet", "Excel Read Sheet", "读取 Excel 指定 sheet 内容。",
                schemaObject(props, new String[] {"fileAbsolutePath"}), annReadOnly());
        }

        private static Map<String, Object> excelWriteToSheetTool() {
            Map<String, Object> props = new HashMap<>();
            props.put("fileAbsolutePath", schemaString("Excel 文件绝对路径"));
            props.put("sheetName", schemaString("Sheet 名称"));
            props.put("newSheet", schemaString("不存在时是否新建 Sheet"));
            props.put("range", schemaString("写入区域（A1 或 A1:C3）"));
            props.put("values", schemaString("写入值：二维数组 JSON 字符串 / TSV / 单值"));
            return toolDef("excel_write_to_sheet", "Excel Write To Sheet", "写入单元格内容到指定 range。",
                schemaObject(props, new String[] {"fileAbsolutePath", "sheetName", "range", "values"}), annWrite());
        }

        private static Map<String, Object> excelCopySheetTool() {
            Map<String, Object> props = new HashMap<>();
            props.put("fileAbsolutePath", schemaString("Excel 文件绝对路径"));
            props.put("srcSheetName", schemaString("源 Sheet 名称"));
            props.put("dstSheetName", schemaString("目标 Sheet 名称"));
            return toolDef("excel_copy_sheet", "Excel Copy Sheet", "复制/覆盖目标 sheet（拷贝值/公式/尽力克隆样式）。",
                schemaObject(props, new String[] {"fileAbsolutePath", "srcSheetName", "dstSheetName"}), annWrite());
        }

        private static Map<String, Object> excelCreateTableTool() {
            Map<String, Object> props = new HashMap<>();
            props.put("fileAbsolutePath", schemaString("Excel 文件绝对路径"));
            props.put("sheetName", schemaString("Sheet 名称"));
            props.put("tableName", schemaString("表格名称"));
            props.put("range", schemaString("表格区域（可选）"));
            return toolDef("excel_create_table", "Excel Create Table", "创建表格语义（NamedRange + AutoFilter）。",
                schemaObject(props, new String[] {"fileAbsolutePath", "sheetName", "tableName"}), annWrite());
        }

        private static Map<String, Object> excelFormatRangeTool() {
            Map<String, Object> props = new HashMap<>();
            props.put("fileAbsolutePath", schemaString("Excel 文件绝对路径"));
            props.put("sheetName", schemaString("Sheet 名称"));
            props.put("range", schemaString("格式化区域"));
            props.put("styles", schemaString("样式定义"));
            return toolDef("excel_format_range", "Excel Format Range", "对 range 内单元格应用简单 CellStyle。",
                schemaObject(props, new String[] {"fileAbsolutePath", "sheetName", "range", "styles"}), annWrite());
        }

        private static Map<String, Object> excelScreenCaptureTool() {
            Map<String, Object> props = new HashMap<>();
            props.put("fileAbsolutePath", schemaString("Excel 文件绝对路径"));
            props.put("sheetName", schemaString("Sheet 名称"));
            props.put("range", schemaString("截图区域（可选）"));
            props.put("maxRows", schemaInt("截图最大行数（可选，默认 20）"));
            props.put("maxCols", schemaInt("截图最大列数（可选，默认 10）"));
            props.put("maxCellTextLen", schemaInt("每格文本最大字符数（可选，默认 18）"));
            return toolDef("excel_screen_capture", "Excel Screen Capture", "渲染简单 PNG（base64）返回。",
                schemaObject(props, new String[] {"fileAbsolutePath", "sheetName"}), annReadOnly());
        }

        private static Map<String, Object> localGetDatabaseInfoTool() {
            return toolDef("local_get_database_info", "Local Get Database Info", "获取库/表列表。",
                schemaObject(new HashMap<>(), new String[0]), annReadOnly());
        }

        private static Map<String, Object> localSqlQueryTool() {
            Map<String, Object> props = new HashMap<>();
            props.put("sql", schemaString("SQL 文本"));
            props.put("maxRows", schemaInt("最多返回行数（默认 50）"));
            props.put("queryTimeoutSeconds", schemaInt("查询超时秒数（可选，默认 10）"));
            props.put("maxOutputChars", schemaInt("最大输出字符数（可选，默认 200000）"));
            props.put("maxCellChars", schemaInt("单元格最大字符数（可选，默认 2000）"));
            return toolDef("local_sql_query", "Local SQL Query", "执行受限 SQL：查询类 + 表级 DDL（CREATE/ALTER/DROP TABLE）。",
                schemaObject(props, new String[] {"sql"}), annWrite());
        }

        private static Map<String, Object> localCheckPermissionsTool() {
            return toolDef("local_check_permissions", "Local Check Permissions", "探测当前数据库只读权限（SHOW/SELECT）。",
                schemaObject(new HashMap<>(), new String[0]), annReadOnly());
        }

        private static Map<String, Object> localGetDdlSqlLogsTool() {
            Map<String, Object> props = new HashMap<>();
            props.put("limit", schemaInt("返回条数（可选）"));
            props.put("offset", schemaInt("偏移量（可选）"));
            return toolDef("local_get_ddl_sql_logs", "Local Get DDL SQL Logs", "获取 DDL SQL 日志。",
                schemaObject(props, new String[0]), annReadOnly());
        }

        private static Map<String, Object> localGetOperationLogsTool() {
            Map<String, Object> props = new HashMap<>();
            props.put("limit", schemaInt("返回条数（可选）"));
            props.put("offset", schemaInt("偏移量（可选）"));
            return toolDef("local_get_operation_logs", "Local Get Operation Logs", "获取操作日志。",
                schemaObject(props, new String[0]), annReadOnly());
        }

        private static Map<String, Object> listAllowedDirectoriesTool() {
            return toolDef("list_allowed_directories", "List Allowed Directories", "列出服务端允许访问的根目录列表。",
                schemaObject(new HashMap<>(), new String[0]), annReadOnly());
        }

        private static Map<String, Object> readTextFileTool() {
            Map<String, Object> props = new HashMap<>();
            props.put("path", schemaString("文件路径（必须位于允许目录内）"));
            props.put("head", schemaInt("仅返回前 N 行（与 tail 互斥）"));
            props.put("tail", schemaInt("仅返回后 N 行（与 head 互斥）"));
            props.put("maxLines", schemaInt("未指定 head/tail 时最多读取行数（可选，默认 1000）"));
            props.put("maxChars", schemaInt("最大输出字符数（可选，默认 200000）"));
            return toolDef("read_text_file", "Read Text File", "读取文本文件内容（UTF-8），默认受 maxLines/maxChars 保护。",
                schemaObject(props, new String[] {"path"}), annReadOnly());
        }

        private static Map<String, Object> readMediaFileTool() {
            Map<String, Object> props = new HashMap<>();
            props.put("path", schemaString("媒体文件路径（必须位于允许目录内）"));
            props.put("maxBytes", schemaInt("读取上限（可选，默认 1048576）"));
            return toolDef("read_media_file", "Read Media File", "读取图片/音频并返回 base64（避免二进制）。",
                schemaObject(props, new String[] {"path"}), annReadOnly());
        }

        private static Map<String, Object> readMultipleFilesTool() {
            Map<String, Object> props = new HashMap<>();
            Map<String, Object> pathsSchema = new HashMap<>();
            pathsSchema.put("type", "array");
            Map<String, Object> itemsSchema = new HashMap<>();
            itemsSchema.put("type", "string");
            pathsSchema.put("items", itemsSchema);
            props.put("paths", pathsSchema);
            props.put("maxChars", schemaInt("每个文件最大字符数（可选，默认 200000）"));
            return toolDef("read_multiple_files", "Read Multiple Files", "批量读取多个文本文件（UTF-8）。",
                schemaObject(props, new String[] {"paths"}), annReadOnly());
        }

        private static Map<String, Object> listDirectoryTool() {
            Map<String, Object> props = new HashMap<>();
            props.put("path", schemaString("目录路径（必须位于允许目录内）"));
            props.put("offset", schemaInt("起始偏移（可选，默认 0）"));
            props.put("limit", schemaInt("最大返回条目数（可选，默认 500）"));
            props.put("maxOutputChars", schemaInt("最大输出字符数（可选，默认 200000）"));
            return toolDef("list_directory", "List Directory", "列出目录下的文件与子目录（支持 offset/limit）。",
                schemaObject(props, new String[] {"path"}), annReadOnly());
        }

        private static Map<String, Object> listDirectoryWithSizesTool() {
            Map<String, Object> props = new HashMap<>();
            props.put("path", schemaString("目录路径（必须位于允许目录内）"));
            props.put("sortBy", schemaString("排序字段（可选）"));
            props.put("offset", schemaInt("起始偏移（可选，默认 0）"));
            props.put("limit", schemaInt("最大返回条目数（可选，默认 500）"));
            props.put("maxOutputChars", schemaInt("最大输出字符数（可选，默认 200000）"));
            return toolDef("list_directory_with_sizes", "List Directory With Sizes", "列出目录及大小（支持 offset/limit）。",
                schemaObject(props, new String[] {"path"}), annReadOnly());
        }

        private static Map<String, Object> directoryTreeTool() {
            Map<String, Object> props = new HashMap<>();
            props.put("path", schemaString("目录路径（必须位于允许目录内）"));
            props.put("excludePatterns", schemaString("排除模式（可选）"));
            props.put("maxDepth", schemaInt("最大递归深度（可选，默认 20）"));
            props.put("maxNodes", schemaInt("最大遍历节点数（可选，默认 20000）"));
            props.put("maxOutputChars", schemaInt("最大输出字符数（可选，默认 200000）"));
            return toolDef("directory_tree", "Directory Tree", "递归目录树（带深度/节点/输出上限）。",
                schemaObject(props, new String[] {"path"}), annReadOnly());
        }

        private static Map<String, Object> moveFileTool() {
            Map<String, Object> props = new HashMap<>();
            props.put("source", schemaString("源路径"));
            props.put("destination", schemaString("目标路径"));
            return toolDef("move_file", "Move File", "移动/重命名文件。",
                schemaObject(props, new String[] {"source", "destination"}), annWrite());
        }

        private static Map<String, Object> getFileInfoTool() {
            Map<String, Object> props = new HashMap<>();
            props.put("path", schemaString("文件或目录路径（必须位于允许目录内）"));
            return toolDef("get_file_info", "Get File Info", "获取文件/目录的基础信息（类型、大小、修改时间等）。",
                schemaObject(props, new String[] {"path"}), annReadOnly());
        }

        private static Map<String, Object> searchFilesTool() {
            Map<String, Object> props = new HashMap<>();
            props.put("path", schemaString("搜索起始目录（必须位于允许目录内）"));
            props.put("pattern", schemaString("匹配模式（支持 * 和 ? 通配；匹配相对路径）"));
            props.put("excludePatterns", schemaString("排除模式（可选，逗号分隔）"));
            props.put("maxDepth", schemaInt("最大递归深度（可选，默认 20）"));
            props.put("maxNodes", schemaInt("最大遍历节点数（可选，默认 20000）"));
            props.put("maxResults", schemaInt("最大匹配结果数（可选，默认 2000）"));
            props.put("maxOutputChars", schemaInt("最大输出字符数（可选，默认 200000）"));
            return toolDef("search_files", "Search Files", "递归搜索文件/目录（带结果/深度/输出上限）。",
                schemaObject(props, new String[] {"path", "pattern"}), annReadOnly());
        }

        private static Map<String, Object> redisGetTool() {
            Map<String, Object> props = new HashMap<>();
            props.put("key", schemaString("Redis key"));
            return toolDef("get", "Redis Get", "获取 Redis key 的字符串值。", schemaObject(props, new String[] {"key"}),
                annReadOnly());
        }

        private static Map<String, Object> redisSetTool() {
            Map<String, Object> props = new HashMap<>();
            props.put("key", schemaString("Redis key"));
            props.put("value", schemaString("Redis value"));
            props.put("expireSeconds", schemaInt("可选：过期秒数（EX）"));
            return toolDef("set", "Redis Set", "设置 Redis key 的字符串值，可选设置过期时间。",
                schemaObject(props, new String[] {"key", "value"}), annWrite());
        }

        private static Map<String, Object> redisDeleteTool() {
            Map<String, Object> props = new HashMap<>();
            // key 支持 string 或 string[]
            Map<String, Object> keyAnyOf = new HashMap<>();
            Map<String, Object> s1 = new HashMap<>();
            s1.put("type", "string");
            Map<String, Object> s2 = new HashMap<>();
            s2.put("type", "array");
            Map<String, Object> items = new HashMap<>();
            items.put("type", "string");
            s2.put("items", items);
            keyAnyOf.put("anyOf", Arrays.asList(s1, s2));
            props.put("key", keyAnyOf);
            return toolDef("delete", "Redis Delete", "删除 Redis key（支持单个或多个）。",
                schemaObject(props, new String[] {"key"}), annWrite());
        }

        private static Map<String, Object> redisListTool() {
            Map<String, Object> props = new HashMap<>();
            props.put("pattern", schemaString("匹配模式（可选，默认 *）"));
            props.put("cursor", schemaString("SCAN 游标（可选，默认 0）"));
            props.put("count", schemaInt("SCAN COUNT（可选，默认 200）"));
            props.put("limit", schemaInt("最大返回 key 数（可选，默认 200）"));
            return toolDef("list", "Redis List", "按 SCAN 分页列出匹配 pattern 的 Redis key。",
                schemaObject(props, new String[0]), annReadOnly());
        }

        private static Map<String, Object> writeFileTool() {
            Map<String, Object> props = new HashMap<>();
            props.put("path", schemaString("文件路径（必须位于允许目录内）"));
            props.put("content", schemaString("写入内容（UTF-8）"));
            return toolDef("write_file", "Write File", "创建或覆盖写入文本文件（UTF-8）。注意：会覆盖同名文件。",
                schemaObject(props, new String[] {"path", "content"}), annWrite());
        }

        private static Map<String, Object> editFileTool() {
            Map<String, Object> props = new HashMap<>();
            props.put("path", schemaString("文件路径（必须位于允许目录内）"));
            props.put("edits", schemaString("编辑指令：可传 JSON 数组（推荐）或纯文本（整文件替换）。"));
            props.put("dryRun", schemaString("是否仅预览（可选）"));
            return toolDef("edit_file", "Edit File", "行级编辑（支持 JSON edits 或整文件替换）。",
                schemaObject(props, new String[] {"path", "edits"}), annWrite());
        }

        private static Map<String, Object> createDirectoryTool() {
            Map<String, Object> props = new HashMap<>();
            props.put("path", schemaString("目录路径（必须位于允许目录内）"));
            return toolDef("create_directory", "Create Directory", "创建目录（若已存在且不是目录则报错）。",
                schemaObject(props, new String[] {"path"}), annWrite());
        }

        private static Map<String, Object> gitLogTool() {
            Map<String, Object> props = new HashMap<>();
            props.put("since", schemaString("起始日期 yyyy-MM-dd（含）"));
            props.put("until", schemaString("结束日期 yyyy-MM-dd（不含次日，可选）"));
            props.put("author", schemaString("作者过滤，匹配 git log --author（可选）"));
            props.put("noMerges", schemaString("是否排除 merge，默认 true"));
            props.put("maxCount", schemaInt("最多条数（默认 500，上限 2000）"));
            props.put("maxOutputChars", schemaInt("最大输出字符（可选）"));
            return toolDef("git_log", "Git Log", "按日期区间列出提交（hash|date|author|subject）。",
                schemaObject(props, new String[] {"since"}), annReadOnly());
        }

        private static Map<String, Object> gitShowTool() {
            Map<String, Object> props = new HashMap<>();
            props.put("commit", schemaString("提交 hash"));
            props.put("statOnly", schemaString("true=仅 --stat，false=含 patch（默认）"));
            props.put("contextLines", schemaInt("patch 上下文行数（默认 3）"));
            props.put("maxOutputChars", schemaInt("最大输出字符（可选）"));
            return toolDef("git_show", "Git Show", "查看单条提交 stat 或 diff。",
                schemaObject(props, new String[] {"commit"}), annReadOnly());
        }

        private static Map<String, Object> gitCommitFilesTool() {
            Map<String, Object> props = new HashMap<>();
            props.put("commit", schemaString("提交 hash"));
            props.put("maxOutputChars", schemaInt("最大输出字符（可选）"));
            return toolDef("git_commit_files", "Git Commit Files", "列出提交变更文件（name-status）。",
                schemaObject(props, new String[] {"commit"}), annReadOnly());
        }

        private static Map<String, Object> gitRunTool() {
            Map<String, Object> props = new HashMap<>();
            Map<String, Object> argsSchema = new HashMap<>();
            argsSchema.put("type", "array");
            Map<String, Object> itemsSchema = new HashMap<>();
            itemsSchema.put("type", "string");
            argsSchema.put("items", itemsSchema);
            props.put("args", argsSchema);
            props.put("maxOutputChars", schemaInt("最大输出字符（可选）"));
            props.put("timeoutSeconds", schemaInt("超时秒数（默认 60）"));
            return toolDef("git_run", "Git Run",
                "执行只读 git 子命令。args 首项须为 log/show/diff/diff-tree/rev-parse/status/branch/config 之一；config 仅读。",
                schemaObject(props, new String[] {"args"}), annReadOnly());
        }

        private static Map<String, Object> describeImageTool() {
            Map<String, Object> props = new HashMap<>();
            props.put("image_path", schemaString("图片文件的绝对路径"));
            props.put("prompt", schemaString("可选的自定义提示语（默认：请详细描述这张图片的内容）"));
            return toolDef("describe_image", "Describe Image",
                "读取图片文件并使用 MiMo v2.5 视觉模型返回详细描述。支持 PNG/JPEG/GIF/WebP/BMP。",
                schemaObject(props, new String[] {"image_path"}), annReadOnly());
        }

        private static Map<String, Object> describeImageBase64Tool() {
            Map<String, Object> props = new HashMap<>();
            props.put("image_data", schemaString("base64 编码的图片数据"));
            props.put("media_type", schemaString("图片 MIME 类型（默认 image/png）"));
            props.put("prompt", schemaString("可选的自定义提示语（默认：请详细描述这张图片的内容）"));
            return toolDef("describe_image_base64", "Describe Image Base64",
                "描述 base64 编码的图片数据。使用 MiMo v2.5 视觉模型。",
                schemaObject(props, new String[] {"image_data"}), annReadOnly());
        }

        private static Map<String, Object> mimoUsageTool() {
            return toolDef("mimo_usage", "MiMo Usage", "查询 MiMo token 使用情况：已用积分、剩余额度、使用百分比。",
                schemaObject(new HashMap<>(), new String[0]), annReadOnly());
        }
    }

    /**
     * 执行 MCP tools/call：从 params 取 name 与 arguments，派发到对应 CommandHandler，返回 content（text item）+ 可选 isError。
     */
    private static Map<String, Object> callTool(CommandDispatcher dispatcher, CommandContext ctx, JsonNode paramsNode) {
        String name = paramsNode == null ? null : McpUtils.text(paramsNode, "name");
        JsonNode args = paramsNode == null ? null : paramsNode.get("arguments");
        if (name == null || name.trim().isEmpty()) {
            return callToolError("params.name不能为空");
        }
        CommandHandler handler = dispatcher.get(name);
        if (handler == null) {
            return callToolError("Tool not found: " + name);
        }
        try {
            CommandResult r = handler.handle(ctx, args);
            String text = r == null ? "" : r.text;
            return callToolText(text, false);
        } catch (IllegalArgumentException e) {
            ctx.log().logMessage("[tools/call] invalidParams tool:" + name + " err:" + e.getMessage());
            return callToolText("Error: " + e.getMessage(), true);
        } catch (Exception e) {
            ctx.log().logMessage("[tools/call] fail tool:" + name + " err:" + e);
            return callToolText("Error: " + simplify(e), true);
        }
    }

    /** 封装 tools/call 的 content 为单条 text；isError 为 true 时在 result 中带 isError。 */
    private static Map<String, Object> callToolText(String text, boolean isError) {
        Map<String, Object> contentItem = new HashMap<>();
        contentItem.put("type", "text");
        contentItem.put("text", text == null ? "" : text);
        List<Map<String, Object>> content = new ArrayList<>();
        content.add(contentItem);

        Map<String, Object> r = new HashMap<>();
        if (isError) {
            r.put("isError", Boolean.TRUE);
        }
        r.put("content", content);
        return r;
    }

    private static Map<String, Object> callToolError(String message) {
        return callToolText("Error: " + message, true);
    }

    /** 组装一条 MCP 工具定义：name、title、description、inputSchema、可选 annotations。 */
    private static Map<String, Object> toolDef(String name, String title, String desc, Map<String, Object> inputSchema,
        Map<String, Object> annotations) {
        Map<String, Object> t = new HashMap<>();
        t.put("name", name);
        t.put("title", title);
        t.put("description", desc);
        t.put("inputSchema", inputSchema);
        if (annotations != null && !annotations.isEmpty()) {
            t.put("annotations", annotations);
        }
        return t;
    }

    /** MCP 工具注解：只读、非破坏、幂等、非开放。 */
    private static Map<String, Object> annReadOnly() {
        Map<String, Object> a = new HashMap<>();
        a.put("readOnlyHint", Boolean.TRUE);
        a.put("destructiveHint", Boolean.FALSE);
        a.put("idempotentHint", Boolean.TRUE);
        a.put("openWorldHint", Boolean.FALSE);
        return a;
    }

    /** MCP 工具注解：可写、破坏性、幂等、非开放。 */
    private static Map<String, Object> annWrite() {
        Map<String, Object> a = new HashMap<>();
        a.put("readOnlyHint", Boolean.FALSE);
        a.put("destructiveHint", Boolean.TRUE);
        a.put("idempotentHint", Boolean.TRUE);
        a.put("openWorldHint", Boolean.FALSE);
        return a;
    }

    /** 构建 JSON Schema type=object，含 properties 与可选 required。 */
    private static Map<String, Object> schemaObject(Map<String, Object> props, String[] required) {
        Map<String, Object> s = new HashMap<>();
        s.put("type", "object");
        s.put("properties", props);
        if (required != null && required.length > 0) {
            List<String> req = new ArrayList<>(Arrays.asList(required));
            s.put("required", req);
        }
        return s;
    }

    /** 构建 JSON Schema type=string，可选 description。 */
    private static Map<String, Object> schemaString(String desc) {
        Map<String, Object> s = new HashMap<>();
        s.put("type", "string");
        if (desc != null && !desc.trim().isEmpty()) {
            s.put("description", desc);
        }
        return s;
    }

    /** 构建 JSON Schema type=integer，可选 description。 */
    private static Map<String, Object> schemaInt(String desc) {
        Map<String, Object> s = new HashMap<>();
        s.put("type", "integer");
        if (desc != null && !desc.trim().isEmpty()) {
            s.put("description", desc);
        }
        return s;
    }

    /** MCP capabilities.tools 的占位对象（listChanged=true）。 */
    private static Map<String, Object> mapOf() {
        Map<String, Object> m = new HashMap<>();
        m.put("listChanged", Boolean.TRUE);
        return m;
    }
}