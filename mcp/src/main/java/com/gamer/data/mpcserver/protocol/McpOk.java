package com.gamer.data.mpcserver.protocol;

import java.util.HashMap;
import java.util.Map;

/**
 * McpOk 表示命令成功
 * 
 * @author liuyunhui
 * @date 2026-03-18
 * @version 1.0
 */
public class McpOk extends McpResponse {

    public Object result;// 结果容器（兼容 MCP：initialize/tools/list/tools/call 等返回体）。

    public McpOk(Object id, Object result) {
        super(id);
        this.result = normalize(result);
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    private static Object normalize(Object result) {
        if (result == null) {
            return null;
        }
        // 兼容旧用法：ok(id, "text") → {"result":{"text":...}}
        if (result instanceof String) {
            Map<String, Object> m = new HashMap<>();
            m.put("text", result);
            return m;
        }
        return result;
    }
}
