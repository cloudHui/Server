package web.config;

import java.io.IOException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import web.service.UserService;

/** Protects browser pages and API endpoints before they reach controllers. */
@Component
public class AuthInterceptor implements HandlerInterceptor {
	private final UserService userService;

	public AuthInterceptor(UserService userService) {
		this.userService = userService;
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
				throws IOException {
		String sessionId = cookieValue(request, "sessionId");
		if (sessionId != null && userService.getSession(sessionId) != null) {
			return true;
		}

		String uri = request.getRequestURI().substring(request.getContextPath().length());
		if (uri.startsWith("/api/")) {
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			response.setContentType("application/json;charset=UTF-8");
			response.getWriter().write("{\"code\":401,\"msg\":\"请先登录\"}");
		} else {
			response.sendRedirect(request.getContextPath() + "/");
		}
		return false;
	}

	private static String cookieValue(HttpServletRequest request, String name) {
		Cookie[] cookies = request.getCookies();
		if (cookies == null) return null;
		for (Cookie cookie : cookies) {
			if (name.equals(cookie.getName())) return cookie.getValue();
		}
		return null;
	}
}
