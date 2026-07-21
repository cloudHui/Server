package lobby.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 用户仓储
 */
public class UserRepository {
	private static final Logger logger = LoggerFactory.getLogger(UserRepository.class);
	private final SqliteDatabase database;

	public UserRepository(SqliteDatabase database) {
		this.database = database;
	}

	public long countUsers() {
		String sql = "SELECT COUNT(1) FROM user";
		try (Connection conn = database.getConnection();
			 PreparedStatement ps = conn.prepareStatement(sql);
			 ResultSet rs = ps.executeQuery()) {
			if (rs.next()) {
				return rs.getLong(1);
			}
		} catch (SQLException e) {
			logger.error("countUsers 失败", e);
		}
		return 0;
	}

	public Optional<UserEntity> findById(long id) {
		String sql = "SELECT * FROM user WHERE id = ?";
		try (Connection conn = database.getConnection();
			 PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setLong(1, id);
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					return Optional.of(map(rs));
				}
			}
		} catch (SQLException e) {
			logger.error("findById 失败, id={}", id, e);
		}
		return Optional.empty();
	}

	public Optional<UserEntity> findByUsername(String username) {
		String sql = "SELECT * FROM user WHERE username = ?";
		try (Connection conn = database.getConnection();
			 PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setString(1, username);
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					return Optional.of(map(rs));
				}
			}
		} catch (SQLException e) {
			logger.error("findByUsername 失败, username={}", username, e);
		}
		return Optional.empty();
	}

	public Optional<UserEntity> findByToken(String token) {
		if (token == null || token.isEmpty()) {
			return Optional.empty();
		}
		String sql = "SELECT * FROM user WHERE token = ?";
		try (Connection conn = database.getConnection();
			 PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setString(1, token);
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					return Optional.of(map(rs));
				}
			}
		} catch (SQLException e) {
			logger.error("findByToken 失败", e);
		}
		return Optional.empty();
	}

	public long insert(UserEntity user) {
		String sql = "INSERT INTO user(username, nickname, password_hash, enabled, token, created_at, last_login_at)"
				+ " VALUES(?,?,?,?,?,?,?)";
		try (Connection conn = database.getConnection();
			 PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
			ps.setString(1, user.getUsername());
			ps.setString(2, user.getNickname());
			ps.setString(3, user.getPasswordHash());
			ps.setInt(4, user.isEnabled() ? 1 : 0);
			ps.setString(5, user.getToken());
			ps.setLong(6, user.getCreatedAt());
			if (user.getLastLoginAt() != null) {
				ps.setLong(7, user.getLastLoginAt());
			} else {
				ps.setObject(7, null);
			}
			ps.executeUpdate();
			try (ResultSet keys = ps.getGeneratedKeys()) {
				if (keys.next()) {
					long id = keys.getLong(1);
					user.setId(id);
					return id;
				}
			}
		} catch (SQLException e) {
			logger.error("insert 用户失败, username={}", user.getUsername(), e);
		}
		return 0;
	}

	public boolean updateLogin(long userId, String token, long lastLoginAt) {
		String sql = "UPDATE user SET token = ?, last_login_at = ? WHERE id = ?";
		try (Connection conn = database.getConnection();
			 PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setString(1, token);
			ps.setLong(2, lastLoginAt);
			ps.setLong(3, userId);
			return ps.executeUpdate() > 0;
		} catch (SQLException e) {
			logger.error("updateLogin 失败, userId={}", userId, e);
			return false;
		}
	}

	public boolean clearToken(long userId) {
		String sql = "UPDATE user SET token = NULL WHERE id = ?";
		try (Connection conn = database.getConnection();
			 PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setLong(1, userId);
			return ps.executeUpdate() > 0;
		} catch (SQLException e) {
			logger.error("clearToken 失败, userId={}", userId, e);
			return false;
		}
	}

	public List<UserEntity> listAll(int limit) {
		List<UserEntity> list = new ArrayList<>();
		int lim = limit <= 0 ? 200 : Math.min(limit, 500);
		String sql = "SELECT * FROM user ORDER BY id DESC LIMIT ?";
		try (Connection conn = database.getConnection();
			 PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setInt(1, lim);
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					list.add(map(rs));
				}
			}
		} catch (SQLException e) {
			logger.error("listAll 失败", e);
		}
		return list;
	}

	public boolean setEnabled(long userId, boolean enabled) {
		String sql = "UPDATE user SET enabled = ? WHERE id = ?";
		try (Connection conn = database.getConnection();
			 PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setInt(1, enabled ? 1 : 0);
			ps.setLong(2, userId);
			return ps.executeUpdate() > 0;
		} catch (SQLException e) {
			logger.error("setEnabled 失败, userId={}", userId, e);
			return false;
		}
	}

	private UserEntity map(ResultSet rs) throws SQLException {
		UserEntity entity = new UserEntity();
		entity.setId(rs.getLong("id"));
		entity.setUsername(rs.getString("username"));
		entity.setNickname(rs.getString("nickname"));
		entity.setPasswordHash(rs.getString("password_hash"));
		entity.setEnabled(rs.getInt("enabled") == 1);
		entity.setToken(rs.getString("token"));
		entity.setCreatedAt(rs.getLong("created_at"));
		long last = rs.getLong("last_login_at");
		if (!rs.wasNull()) {
			entity.setLastLoginAt(last);
		}
		return entity;
	}
}
