package db.mysql;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

/**
 * 数据库SessionFactory工厂（单例）
 * 按配置文件名和环境缓存SqlSessionFactory实例
 */
public class DBSourceFactory {
	public static final DBSourceFactory INSTANCE = new DBSourceFactory();
	private final Map<String, EnvNodes> envNodesMap = new HashMap<>();

	private DBSourceFactory() {}

	/** 获取默认SessionFactory */
	public SqlSessionFactory getSqlSessionFactory() {
		String defaultName = "default";
		return this.getSqlSessionFactory(defaultName + "_druid.xml", defaultName);
	}

	/** 按配置名和环境获取SessionFactory */
	public SqlSessionFactory getSqlSessionFactory(String configName, String env) {
		EnvNodes envNodes = this.envNodesMap.get(configName);
		if (null == envNodes) {
			this.envNodesMap.putIfAbsent(configName, new EnvNodes(configName));
			envNodes = this.envNodesMap.get(configName);
		}

		return envNodes.getSqlSessionFactory(env);
	}

	/** 从MyBatis配置文件创建SessionFactory */
	private SqlSessionFactory createSqlSessionFactory(String name, String env) {
		try {
			Reader reader = Resources.getResourceAsReader(name);
			return (new SqlSessionFactoryBuilder()).build(reader, env);
		} catch (Exception var4) {
			throw new RuntimeException("error! failed for create session factory:" + name, var4);
		}
	}

	/** 环境节点：按环境名缓存SessionFactory（双重检查锁） */
	private class EnvNodes {
		private final String configName;
		private final Object lock;
		private Map<String, SqlSessionFactory> sqlSessionFactoryMap;

		public EnvNodes(String configName) {
			this.configName = configName;
			this.lock = new Object();
			this.sqlSessionFactoryMap = new HashMap<>();
		}

		public SqlSessionFactory getSqlSessionFactory(String env) {
			SqlSessionFactory sqlSessionFactory = this.sqlSessionFactoryMap.get(env);
			if (null == sqlSessionFactory) {
				synchronized (this.lock) {
					sqlSessionFactory = this.sqlSessionFactoryMap.get(env);
					if (null == sqlSessionFactory) {
						sqlSessionFactory = DBSourceFactory.this.createSqlSessionFactory(this.configName, env);
						this.sqlSessionFactoryMap.put(env, sqlSessionFactory);
					}
				}
			}

			return sqlSessionFactory;
		}
	}
}
