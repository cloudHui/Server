package game.db;

import game.manager.table.Table;
import game.manager.table.TableUser;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 将每局结算按玩家写入大厅共享的 SQLite 数据库。 */
public class ScoreRepository {
	private static final Logger logger = LoggerFactory.getLogger(ScoreRepository.class);
	private static volatile ScoreRepository instance;
	private final String jdbcUrl;

	public ScoreRepository(String dbPath) {
		try {
			Class.forName("org.sqlite.JDBC");
		} catch (ClassNotFoundException e) {
			throw new IllegalStateException("sqlite-jdbc 未找到", e);
		}
		File file = new File(dbPath);
		File parent = file.getParentFile();
		if (parent != null && !parent.exists()) parent.mkdirs();
		jdbcUrl = "jdbc:sqlite:" + file.getAbsolutePath();
		initSchema();
	}

	public static void initialize(String dbPath) {
		instance = new ScoreRepository(dbPath);
	}

	public static ScoreRepository getInstance() {
		ScoreRepository value = instance;
		if (value == null) {
			synchronized (ScoreRepository.class) {
				value = instance;
				if (value == null) instance = value = new ScoreRepository("../lobby/data/lobby.db");
			}
		}
		return value;
	}

	public void saveRound(Table table) {
		String sql = "INSERT OR REPLACE INTO score_record(table_id, room_id, game_type, round, user_id, seat, score, total_score, winner_seat, score_value, win_type, created_at)"
				+ " VALUES(?,?,?,?,?,?,?,?,?,?,?,?)";
		int winnerSeat = table.getGameResult().getRoundEntries().get(table.getGameResult().getRoundEntries().size() - 1).getWinnerSeat();
		game.manager.table.GameResult.RoundEntry entry = table.getGameResult().getRoundEntries()
				.get(table.getGameResult().getRoundEntries().size() - 1);
		try (Connection conn = DriverManager.getConnection(jdbcUrl);
			 PreparedStatement ps = conn.prepareStatement(sql)) {
			conn.setAutoCommit(false);
			for (int seat = 0; seat < table.getTableModel().getSeatNum(); seat++) {
				TableUser user = table.getSeatUser(seat);
				if (user == null) continue;
				ps.setLong(1, table.getTableId());
				ps.setInt(2, table.getRoomId());
				ps.setInt(3, table.getGameType());
				ps.setInt(4, entry.getRound());
				ps.setInt(5, user.getUserId());
				ps.setInt(6, seat);
				ps.setInt(7, entry.getScores()[seat]);
				ps.setInt(8, table.getGameResult().getTotalScore(seat));
				ps.setInt(9, winnerSeat);
				ps.setInt(10, entry.getScore());
				ps.setString(11, entry.getWinType());
				ps.setLong(12, System.currentTimeMillis());
				ps.addBatch();
			}
			ps.executeBatch();
			conn.commit();
		} catch (SQLException e) {
			logger.error("保存战绩失败, tableId={}, round={}", table.getTableId(), entry.getRound(), e);
		}
	}

	private void initSchema() {
		String sql = "CREATE TABLE IF NOT EXISTS score_record ("
				+ "table_id INTEGER NOT NULL, room_id INTEGER NOT NULL, game_type INTEGER NOT NULL,"
				+ "round INTEGER NOT NULL, user_id INTEGER NOT NULL, seat INTEGER NOT NULL,"
				+ "score INTEGER NOT NULL, total_score INTEGER NOT NULL, winner_seat INTEGER NOT NULL,"
				+ "score_value INTEGER NOT NULL, win_type TEXT NOT NULL, created_at INTEGER NOT NULL,"
				+ "PRIMARY KEY(table_id, round, user_id))";
		try (Connection conn = DriverManager.getConnection(jdbcUrl); Statement st = conn.createStatement()) {
			st.execute(sql);
		} catch (SQLException e) {
			throw new IllegalStateException("初始化战绩表失败", e);
		}
	}
}
