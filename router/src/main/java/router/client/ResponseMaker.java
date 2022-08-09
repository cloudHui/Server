package router.client;

import http.handler.HttpResponseMaker;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.utils.JsonUtils;

public class ResponseMaker<T> implements HttpResponseMaker<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ResponseMaker.class);
    private static final String CONTENT_TYPE_JSON_URL = "application/json;charset=utf-8";

    public ResponseMaker() {
    }

    public FullHttpResponse wrap(int msgId, T s) {
        return null;
    }

    public FullHttpResponse wrap(T s) {
        if (s instanceof String) {
            return this.wrap((String)s);
        } else {
            return !s.getClass().isPrimitive() && !(s instanceof Number) ? this.wrap(JsonUtils.writeValue(s)) : this.wrap(String.valueOf(s));
        }
    }

    public FullHttpResponse wrap(String content) {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.copiedBuffer(content, CharsetUtil.UTF_8));
        response.headers().set("Content-Type", CONTENT_TYPE_JSON_URL);
        response.headers().set("Access-Control-Allow-Origin", "*");
        response.headers().set("Content-Length", response.content().readableBytes());
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("WRAP:{}", content);
        }

        return response;
    }
}
