package lobby.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 后台查询游戏服写入的战绩。 */
public class ScoreQueryRepository {
	private static final Logger logger = LoggerFactory.getLogger(ScoreQueryRepository.class);
	private final SqliteDatabase database;

	public ScoreQueryRepository(SqliteDatabase database) { this.database = database; }

	public List<Map<String, Object>> list(int offset, int limit) {
		List<Map<String, Object>> rows = new ArrayList<>();
		String sql = "SELECT table_id, room_id, game_type, round, user_id, seat, score, total_score, winner_seat, score_value, win_type, created_at"
				+ " FROM score_record ORDER BY created_at DESC LIMIT ? OFFSET ?";
		try (Connection conn = database.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setInt(1, Math.min(Math.max(limit, 1), 100));
			ps.setInt(2, Math.max(offset, 0));
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					Map<String, Object> row = new LinkedHashMap<>();
					row.put("tableId", rs.getLong("table_id"));
					row.put("roomId", rs.getInt("room_id"));
					row.put("gameType", rs.getInt("game_type"));
					row.put("round", rs.getInt("round"));
					row.put("userId", rs.getInt("user_id"));
					row.put("seat", rs.getInt("seat"));
					row.put("score", rs.getInt("score"));
					row.put("totalScore", rs.getInt("total_score"));
					row.put("winnerSeat", rs.getInt("winner_seat"));
					row.put("scoreValue", rs.getInt("score_value"));
					row.put("winType", rs.getString("win_type"));
					row.put("createdAt", rs.getLong("created_at"));
					rows.add(row);
				}
			}
		} catch (SQLException e) {
			logger.error("查询战绩失败", e);
		}
		return rows;
	}
}
