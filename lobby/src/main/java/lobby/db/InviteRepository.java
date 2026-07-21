package lobby.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 邀请码仓储
 */
public class InviteRepository {
	private static final Logger logger = LoggerFactory.getLogger(InviteRepository.class);
	private final SqliteDatabase database;

	public InviteRepository(SqliteDatabase database) {
		this.database = database;
	}

	public InviteEntity create(String note, String createdBy, Long expiresAt, int maxUses) {
		InviteEntity entity = new InviteEntity();
		entity.setToken(UUID.randomUUID().toString().replace("-", ""));
		entity.setNote(note);
		entity.setCreatedBy(createdBy);
		entity.setCreatedAt(System.currentTimeMillis());
		entity.setExpiresAt(expiresAt);
		entity.setMaxUses(maxUses <= 0 ? 1 : maxUses);
		entity.setUsedCount(0);
		entity.setEnabled(true);

		String sql = "INSERT INTO invite(token, note, created_by, created_at, expires_at, max_uses, used_count, enabled)"
				+ " VALUES(?,?,?,?,?,?,?,?)";
		try (Connection conn = database.getConnection();
			 PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
			ps.setString(1, entity.getToken());
			ps.setString(2, entity.getNote());
			ps.setString(3, entity.getCreatedBy());
			ps.setLong(4, entity.getCreatedAt());
			if (entity.getExpiresAt() != null) {
				ps.setLong(5, entity.getExpiresAt());
			} else {
				ps.setObject(5, null);
			}
			ps.setInt(6, entity.getMaxUses());
			ps.setInt(7, entity.getUsedCount());
			ps.setInt(8, 1);
			ps.executeUpdate();
			try (ResultSet keys = ps.getGeneratedKeys()) {
				if (keys.next()) {
					entity.setId(keys.getLong(1));
				}
			}
			logger.info("创建邀请码, token={}, maxUses={}", mask(entity.getToken()), entity.getMaxUses());
			return entity;
		} catch (SQLException e) {
			logger.error("创建邀请码失败", e);
			return null;
		}
	}

	public List<InviteEntity> listAll() {
		List<InviteEntity> list = new ArrayList<>();
		String sql = "SELECT * FROM invite ORDER BY id DESC";
		try (Connection conn = database.getConnection();
			 PreparedStatement ps = conn.prepareStatement(sql);
			 ResultSet rs = ps.executeQuery()) {
			while (rs.next()) {
				list.add(map(rs));
			}
		} catch (SQLException e) {
			logger.error("listAll 邀请码失败", e);
		}
		return list;
	}

	public boolean revoke(String token) {
		String sql = "UPDATE invite SET enabled = 0 WHERE token = ?";
		try (Connection conn = database.getConnection();
			 PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setString(1, token);
			return ps.executeUpdate() > 0;
		} catch (SQLException e) {
			logger.error("revoke 失败, token={}", mask(token), e);
			return false;
		}
	}

	public Optional<InviteEntity> peekValid(String token) {
		if (token == null || token.isEmpty()) {
			return Optional.empty();
		}
		String sql = "SELECT * FROM invite WHERE token = ?";
		try (Connection conn = database.getConnection();
			 PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setString(1, token);
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					InviteEntity entity = map(rs);
					if (entity.isValidNow()) {
						return Optional.of(entity);
					}
				}
			}
		} catch (SQLException e) {
			logger.error("peekValid 失败", e);
		}
		return Optional.empty();
	}

	/**
	 * 消费邀请码（原子递增 used_count）
	 * @return true 消费成功
	 */
	public boolean consume(String token) {
		if (token == null || token.isEmpty()) {
			return false;
		}
		long now = System.currentTimeMillis();
		String sql = "UPDATE invite SET used_count = used_count + 1 WHERE token = ?"
				+ " AND enabled = 1"
				+ " AND used_count < max_uses"
				+ " AND (expires_at IS NULL OR expires_at = 0 OR expires_at > ?)";
		try (Connection conn = database.getConnection();
			 PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setString(1, token);
			ps.setLong(2, now);
			int updated = ps.executeUpdate();
			if (updated > 0) {
				logger.info("消费邀请码成功, token={}", mask(token));
				return true;
			}
			logger.warn("消费邀请码失败(无效或已用尽), token={}", mask(token));
			return false;
		} catch (SQLException e) {
			logger.error("consume 失败", e);
			return false;
		}
	}

	public long countInvites() {
		String sql = "SELECT COUNT(1) FROM invite";
		try (Connection conn = database.getConnection();
			 PreparedStatement ps = conn.prepareStatement(sql);
			 ResultSet rs = ps.executeQuery()) {
			if (rs.next()) {
				return rs.getLong(1);
			}
		} catch (SQLException e) {
			logger.error("countInvites 失败", e);
		}
		return 0;
	}

	private InviteEntity map(ResultSet rs) throws SQLException {
		InviteEntity entity = new InviteEntity();
		entity.setId(rs.getLong("id"));
		entity.setToken(rs.getString("token"));
		entity.setNote(rs.getString("note"));
		entity.setCreatedBy(rs.getString("created_by"));
		entity.setCreatedAt(rs.getLong("created_at"));
		long expires = rs.getLong("expires_at");
		if (!rs.wasNull()) {
			entity.setExpiresAt(expires);
		}
		entity.setMaxUses(rs.getInt("max_uses"));
		entity.setUsedCount(rs.getInt("used_count"));
		entity.setEnabled(rs.getInt("enabled") == 1);
		return entity;
	}

	private static String mask(String token) {
		if (token == null) {
			return "null";
		}
		return token.length() <= 8 ? token : token.substring(0, 8) + "...";
	}
}
