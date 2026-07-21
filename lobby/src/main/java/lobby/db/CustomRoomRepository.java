package lobby.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import model.tablemodel.TableModel;
import model.tablemodel.TableModelJson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 持久化自定义房间模板。 */
public class CustomRoomRepository {
	private static final Logger logger = LoggerFactory.getLogger(CustomRoomRepository.class);
	private final SqliteDatabase database;

	public CustomRoomRepository(SqliteDatabase database) {
		this.database = database;
	}

	public boolean save(TableModel model, String createdBy) {
		if (model == null || model.getId() < 10000) return false;
		String sql = "INSERT OR REPLACE INTO custom_room(model_id, model_json, game_type, created_by, created_at, enabled)"
				+ " VALUES(?,?,?,?,?,1)";
		try (Connection conn = database.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setInt(1, model.getId());
			ps.setString(2, TableModelJson.toJson(model));
			ps.setInt(3, model.getType());
			ps.setString(4, createdBy);
			ps.setLong(5, System.currentTimeMillis());
			return ps.executeUpdate() == 1;
		} catch (SQLException e) {
			logger.error("保存自定义房间模板失败, modelId={}", model.getId(), e);
			return false;
		}
	}

	public List<TableModel> listEnabled() {
		List<TableModel> models = new ArrayList<>();
		String sql = "SELECT model_json FROM custom_room WHERE enabled = 1 ORDER BY model_id";
		try (Connection conn = database.getConnection(); PreparedStatement ps = conn.prepareStatement(sql);
			 ResultSet rs = ps.executeQuery()) {
			while (rs.next()) {
				TableModel model = TableModelJson.parse(rs.getString(1));
				if (model != null && model.getId() >= 10000) models.add(model);
			}
		} catch (SQLException e) {
			logger.error("加载自定义房间模板失败", e);
		}
		return models;
	}
}
