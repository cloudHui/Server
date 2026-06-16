package com.gamer.data.mpcserver.protocol;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * MCP 响应对象：入口将其序列化为 JSON 输出到 stdout（一行一条）。
 *
 * <p>
 * 约定：
 * </p>
 * <ul>
 * <li>成功：返回 result（当前仅 text）。</li>
 * <li>失败：返回 error（code/message）。</li>
 * <li>id：与请求 id 保持一致，便于调用方关联请求/响应。</li>
 * </ul>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class McpResponse {
    public McpResponse(Object id) {
        this.id = id;
    }

    public String jsonrpc = "2.0";// jsonrpc版本 不能删返回需要
    public Object id;// 必须与请求的 id 保持一致（可为数字/字符串/null）。

    public String getJsonrpc() {
        return jsonrpc;
    }

    public Object getId() {
        return id;
    }

    public void setId(Object id) {
        this.id = id;
    }

    /**
     * 成功响应工厂。
     *
     * @param id
     *            请求 id（可为 null）
     * @param text
     *            结果文本
     * @return 成功响应
     */
    public static McpResponse ok(Object id, String text) {
        return new McpOk(id, text);
    }

    /**
     * 成功响应工厂（允许返回任意结构体）。
     *
     * <p>
     * 用于 MCP 标准方法：initialize / tools/list / tools/call。
     * </p>
     *
     * @param id
     *            请求 id（可为 null）
     * @param result
     *            结果对象（会被 JSON 序列化）
     * @return 成功响应
     */
    public static McpResponse okResult(Object id, Object result) {
        return new McpOk(id, result);
    }

    /**
     * 失败响应工厂。
     *
     * @param id
     *            请求 id（可为 null）
     * @param code
     *            错误码
     * @param message
     *            错误信息
     * @return 失败响应
     */
    public static McpResponse fail(Object id, int code, String message) {
        return new McpError(id, code, message);
    }

    @Override
    public String toString() {
        return "McpResponse{id=" + id + "}";
    }
}
