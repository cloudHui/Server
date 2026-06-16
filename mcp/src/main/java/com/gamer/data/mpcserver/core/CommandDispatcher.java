package com.gamer.data.mpcserver.core;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.gamer.data.mpcserver.commands.CommandHandler;

/**
 * 命令分发器：维护 method → handler 的映射，并提供查询与列举能力。
 *
 * <p>
 * 该类本身不做任何 I/O；只负责 method 校验、注册与获取。
 * </p>
 */
public class CommandDispatcher {

    private static final Map<String, CommandHandler> INIT = new HashMap<>();

    static {
        // 注册method 对应的处理器。
        HandleTypeRegister.initSetFactory(CommandHandler.class, INIT);
    }

    /**
     * 获取指定 method 的处理器。
     *
     * @param method
     *            method 名称
     * @return 处理器；不存在时返回 null
     */
    public CommandHandler get(String method) {
        return INIT.get(method);
    }

    /**
     * 列出当前已注册的所有 method。
     *
     * <p>
     * 返回的是快照集合，避免外部修改影响内部状态。
     * </p>
     *
     * @return method 集合快照
     */
    public Set<String> methods() {
        return new HashSet<>(INIT.keySet());
    }
}
