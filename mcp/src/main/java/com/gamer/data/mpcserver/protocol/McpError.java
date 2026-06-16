package com.gamer.data.mpcserver.protocol;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * MCP 错误对象。
 *
 * <p>
 * code 为机器可读的错误码，message 为人类可读的简要描述。
 * </p>
 * <p>
 * 当前错误码由入口统一生成，例如：PARSE_ERROR、INVALID_REQUEST、METHOD_NOT_FOUND、
 * INVALID_PARAMS、INTERNAL_ERROR。
 * </p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class McpError extends McpResponse {

    public ErrorBody error;

    public McpError(Object id, int code, String message) {
        super(id);
        this.error = new ErrorBody(code, message);
    }

    public ErrorBody getError() {
        return error;
    }

    public void setError(ErrorBody error) {
        this.error = error;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ErrorBody {
        public int code;// 必须是数字类型
        public String message;

        public ErrorBody(int code, String message) {
            this.code = code;
            this.message = message;
        }
    }

    @Override
    public String toString() {
        return "McpError{id=" + id + ", error=" + error.code + ", message=" + error.message + "}";
    }
}
