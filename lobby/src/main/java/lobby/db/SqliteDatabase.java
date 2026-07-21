package lobby.db;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lobby SQLite 连接与建表
 */
public class SqliteDatabase {
	private static final Logger logger = LoggerFactory.getLogger(SqliteDatabase.class);
	private static final String DB_PATH = "data/lobby.db";

	private static SqliteDatabase instance;
	private final String jdbcUrl;

	private SqliteDatabase() {
		try {
			Class.forName("org.sqlite.JDBC");
		} catch (ClassNotFoundException e) {
			throw new RuntimeException("sqlite-jdbc 未找到", e);
		}
		File dbFile = new File(DB_PATH);
		File parent = dbFile.getParentFile();
		if (parent != null && !parent.exists() && !parent.mkdirs()) {
			throw new RuntimeException("无法创建数据库目录: " + parent.getAbsolutePath());
		}
		this.jdbcUrl = "jdbc:sqlite:" + dbFile.getAbsolutePath();
		logger.info("SQLite 数据库路径: {}", dbFile.getAbsolutePath());
	}

	public static synchronized SqliteDatabase getInstance() {
		if (instance == null) {
			instance = new SqliteDatabase();
		}
		return instance;
	}

	public Connection getConnection() throws SQLException {
		return DriverManager.getConnection(jdbcUrl);
	}

	public void initSchema() {
		String userSql = "CREATE TABLE IF NOT EXISTS user ("
				+ "id INTEGER PRIMARY KEY AUTOINCREMENT,"
				+ "username TEXT NOT NULL UNIQUE,"
				+ "nickname TEXT NOT NULL,"
				+ "password_hash TEXT NOT NULL,"
				+ "enabled INTEGER NOT NULL DEFAULT 1,"
				+ "token TEXT,"
				+ "created_at INTEGER NOT NULL,"
				+ "last_login_at INTEGER"
				+ ")";
		String inviteSql = "CREATE TABLE IF NOT EXISTS invite ("
				+ "id INTEGER PRIMARY KEY AUTOINCREMENT,"
				+ "token TEXT NOT NULL UNIQUE,"
				+ "note TEXT,"
				+ "created_by TEXT,"
				+ "created_at INTEGER NOT NULL,"
				+ "expires_at INTEGER,"
				+ "max_uses INTEGER NOT NULL DEFAULT 1,"
				+ "used_count INTEGER NOT NULL DEFAULT 0,"
				+ "enabled INTEGER NOT NULL DEFAULT 1"
				+ ")";
		try (Connection conn = getConnection(); Statement st = conn.createStatement()) {
			st.execute(userSql);
			st.execute(inviteSql);
			logger.info("SQLite 表结构初始化完成");
		} catch (SQLException e) {
			throw new RuntimeException("初始化 SQLite 表失败", e);
		}
	}
}
