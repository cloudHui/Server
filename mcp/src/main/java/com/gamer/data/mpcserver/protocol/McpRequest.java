package com.gamer.data.mpcserver.protocol;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * MCP 请求对象：由 stdin 输入的一行 JSON 反序列化得到。
 *
 * <p>字段说明：</p>
 * <ul>
 *   <li>id：请求标识，原样回传给响应；允许为空（入口会传 null）。</li>
 *   <li>method：命令名（必填），由 {@code CommandDispatcher} 分发。</li>
 *   <li>params：命令参数（可选），具体结构由各命令自行解释。</li>
 * </ul>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class McpRequest {
    private String jsonrpc; // 对应 JSON 中的 "jsonrpc"
    public Object id;//请求标识，原样回传给响应；允许为空（入口会传 null）。
    public String method;//命令名（必填），由 {@code CommandDispatcher} 分发。
    public JsonNode params;//命令参数（可选），具体结构由各命令自行解释。


    public String getJsonrpc() {
        return jsonrpc;
    }

    public void setJsonrpc(String jsonrpc) {
        this.jsonrpc = jsonrpc;
    }

    public Object getId() {
        return id;
    }

    public void setId(Object id) {
        this.id = id;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public JsonNode getParams() {
        return params;
    }

    public void setParams(JsonNode params) {
        this.params = params;
    }

    @Override
    public String toString() {
        return "McpRequest{jsonrpc=" + jsonrpc + ", id=" + id + ", method=" + method + ", params=" + params + "}";
    }
}

