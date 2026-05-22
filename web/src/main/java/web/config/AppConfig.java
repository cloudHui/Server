package web.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import web.service.GateClient;

/**
 * 应用配置
 */
@Configuration
public class AppConfig {
	private static final Logger logger = LoggerFactory.getLogger(AppConfig.class);

	@Value("${gate.host:127.0.0.1}")
	private String gateHost;

	@Value("${gate.port:9001}")
	private int gatePort;

	@Bean
	public GateClient gateClient() {
		logger.info("初始化Gate客户端, gate: {}:{}", gateHost, gatePort);
		return new GateClient(gateHost, gatePort);
	}
}
