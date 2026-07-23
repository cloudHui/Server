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
	private final DatabaseExecutorManager databaseExecutorManager;

	public ScoreRepository(String dbPath) {
		this(dbPath, new DatabaseExecutorManager(1));
	}

	public ScoreRepository(String dbPath, DatabaseExecutorManager databaseExecutorManager) {
		this.databaseExecutorManager = databaseExecutorManager;
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

	public static void initialize(String dbPath, DatabaseExecutorManager databaseExecutorManager) {
		instance = new ScoreRepository(dbPath, databaseExecutorManager);
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
		RoundSnapshot snapshot = createSnapshot(table);
		if (snapshot == null) return;
		databaseExecutorManager.submit(() -> writeRound(snapshot)).exceptionally(error -> {
			logger.error("保存战绩异步任务失败, tableId={}", snapshot.tableId, error);
			return null;
		});
	}

	/** 在桌子线程复制结算数据，数据库线程只接触不可变快照。 */
	private RoundSnapshot createSnapshot(Table table) {
		if (table.getGameResult().getRoundEntries().isEmpty()) return null;
		game.manager.table.GameResult.RoundEntry entry = table.getGameResult().getRoundEntries()
				.get(table.getGameResult().getRoundEntries().size() - 1);
		java.util.List<ScoreRow> rows = new java.util.ArrayList<>();
		for (int seat = 0; seat < table.getTableModel().getSeatNum(); seat++) {
			TableUser user = table.getSeatUser(seat);
			if (user != null) rows.add(new ScoreRow(user.getUserId(), seat, entry.getScores()[seat],
					table.getGameResult().getTotalScore(seat)));
		}
		return new RoundSnapshot(table.getTableId(), table.getRoomId(), table.getGameType(), entry.getRound(),
				entry.getWinnerSeat(), entry.getScore(), entry.getWinType(), System.currentTimeMillis(), rows);
	}

	private void writeRound(RoundSnapshot snapshot) {
		String sql = "INSERT OR REPLACE INTO score_record(table_id, room_id, game_type, round, user_id, seat, score, total_score, winner_seat, score_value, win_type, created_at)"
				+ " VALUES(?,?,?,?,?,?,?,?,?,?,?,?)";
		try (Connection conn = DriverManager.getConnection(jdbcUrl);
				 PreparedStatement ps = conn.prepareStatement(sql)) {
			conn.setAutoCommit(false);
			for (ScoreRow row : snapshot.rows) {
				ps.setLong(1, snapshot.tableId); ps.setInt(2, snapshot.roomId); ps.setInt(3, snapshot.gameType);
				ps.setInt(4, snapshot.round); ps.setInt(5, row.userId); ps.setInt(6, row.seat);
				ps.setInt(7, row.score); ps.setInt(8, row.totalScore); ps.setInt(9, snapshot.winnerSeat);
				ps.setInt(10, snapshot.scoreValue); ps.setString(11, snapshot.winType);
				ps.setLong(12, snapshot.createdAt);
				ps.addBatch();
			}
			ps.executeBatch();
			conn.commit();
		} catch (SQLException e) {
			logger.error("保存战绩失败, tableId={}, round={}", snapshot.tableId, snapshot.round, e);
		}
	}

	private static final class RoundSnapshot {
		private final long tableId; private final int roomId; private final int gameType; private final int round;
		private final int winnerSeat; private final int scoreValue; private final String winType;
		private final long createdAt; private final java.util.List<ScoreRow> rows;
		private RoundSnapshot(long tableId, int roomId, int gameType, int round, int winnerSeat,
				int scoreValue, String winType, long createdAt, java.util.List<ScoreRow> rows) {
			this.tableId = tableId; this.roomId = roomId; this.gameType = gameType; this.round = round;
			this.winnerSeat = winnerSeat; this.scoreValue = scoreValue; this.winType = winType;
			this.createdAt = createdAt; this.rows = java.util.Collections.unmodifiableList(rows);
		}
	}

	private static final class ScoreRow {
		private final int userId; private final int seat; private final int score; private final int totalScore;
		private ScoreRow(int userId, int seat, int score, int totalScore) {
			this.userId = userId; this.seat = seat; this.score = score; this.totalScore = totalScore;
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
