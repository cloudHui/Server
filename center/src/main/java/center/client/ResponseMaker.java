package center.client;

import http.handler.HttpResponseMaker;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.other.JsonUtils;

/**
 * HTTP响应构建器
 * 负责将Java对象转换为HTTP响应
 */
public class ResponseMaker<T> implements HttpResponseMaker<T> {
	private static final Logger logger = LoggerFactory.getLogger(ResponseMaker.class);
	private static final String CONTENT_TYPE_JSON = "application/json;charset=utf-8";

	public ResponseMaker() {
		logger.debug("创建HTTP响应构建器");
	}

	@Override
	public FullHttpResponse wrap(int messageId, T content) {
		// 不支持消息ID包装,返回null
		return null;
	}

	@Override
	public FullHttpResponse wrap(T content) {
		try {
			if (content instanceof String) {
				return wrap((String) content);
			} else if (content.getClass().isPrimitive() || content instanceof Number) {
				return wrap(String.valueOf(content));
			} else {
				return wrap(JsonUtils.writeValue(content));
			}
		} catch (Exception e) {
			logger.error("包装HTTP响应失败", e);
			return createErrorResponse();
		}
	}

	/**
	 * 包装字符串内容为HTTP响应
	 */
	@Override
	public FullHttpResponse wrap(String content) {
		try {
			FullHttpResponse response = new DefaultFullHttpResponse(
					HttpVersion.HTTP_1_1,
					HttpResponseStatus.OK,
					Unpooled.copiedBuffer(content, CharsetUtil.UTF_8)
			);

			// 设置响应头
			response.headers().set("Content-Type", CONTENT_TYPE_JSON);
			response.headers().set("Access-Control-Allow-Origin", "*");
			response.headers().set("Content-Length", response.content().readableBytes());

			if (logger.isDebugEnabled()) {
				logger.debug("构建HTTP响应, 内容长度: {}", content.length());
			}

			return response;
		} catch (Exception e) {
			logger.error("构建HTTP响应失败", e);
			return createErrorResponse();
		}
	}

	/**
	 * 创建错误响应
	 */
	private FullHttpResponse createErrorResponse() {
		FullHttpResponse response = new DefaultFullHttpResponse(
				HttpVersion.HTTP_1_1,
				HttpResponseStatus.INTERNAL_SERVER_ERROR
		);
		response.headers().set("Content-Type", CONTENT_TYPE_JSON);
		return response;
	}
}