package web.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import web.service.GateClient;

/**
 * 应用配置
 */
@Configuration
public class AppConfig implements WebMvcConfigurer {
	private static final Logger logger = LoggerFactory.getLogger(AppConfig.class);

	@Value("${gate.host:127.0.0.1}")
	private String gateHost;

	@Value("${gate.port:5600}")
	private int gatePort;

	private final AuthInterceptor authInterceptor;

	public AppConfig(AuthInterceptor authInterceptor) {
		this.authInterceptor = authInterceptor;
	}

	@Bean
	public GateClient gateClient() {
		logger.info("初始化Gate客户端, gate: {}:{}", gateHost, gatePort);
		return new GateClient(gateHost, gatePort);
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(authInterceptor)
				.addPathPatterns("/**")
				.excludePathPatterns("/", "/index.html", "/api/auth/**", "/api/login",
						"/app-base.js", "/favicon.ico", "/css/**", "/js/**", "/img/**",
						"/ws/**");
	}
}
