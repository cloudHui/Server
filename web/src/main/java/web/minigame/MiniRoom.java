package web.minigame;

import java.util.UUID;

/**
 * 休闲小游戏房间（五子棋 / 象棋 1v1）
 */
public class MiniRoom {
	public enum GameType {
		GOMOKU, CHESS
	}

	private final String roomId = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
	private final GameType gameType;
	private final String playerASession;
	private final int playerAUserId;
	private final String playerAName;
	private final String playerBSession;
	private final int playerBUserId;
	private final String playerBName;

	private final GomokuBoard gomoku;
	private final ChessBoard chess;

	public MiniRoom(GameType gameType,
					String aSession, int aUserId, String aName,
					String bSession, int bUserId, String bName) {
		this.gameType = gameType;
		this.playerASession = aSession;
		this.playerAUserId = aUserId;
		this.playerAName = aName == null || aName.isEmpty() ? ("玩家" + aUserId) : aName;
		this.playerBSession = bSession;
		this.playerBUserId = bUserId;
		this.playerBName = bName == null || bName.isEmpty() ? ("玩家" + bUserId) : bName;
		this.gomoku = gameType == GameType.GOMOKU ? new GomokuBoard() : null;
		this.chess = gameType == GameType.CHESS ? new ChessBoard() : null;
	}

	public String getRoomId() {
		return roomId;
	}

	public GameType getGameType() {
		return gameType;
	}

	public String getPlayerASession() {
		return playerASession;
	}

	public String getPlayerBSession() {
		return playerBSession;
	}

	public int getPlayerAUserId() {
		return playerAUserId;
	}

	public int getPlayerBUserId() {
		return playerBUserId;
	}

	public String getPlayerAName() {
		return playerAName;
	}

	public String getPlayerBName() {
		return playerBName;
	}

	public GomokuBoard getGomoku() {
		return gomoku;
	}

	public ChessBoard getChess() {
		return chess;
	}

	public boolean containsSession(String sessionId) {
		return playerASession.equals(sessionId) || playerBSession.equals(sessionId);
	}

	public String opponentSession(String sessionId) {
		if (playerASession.equals(sessionId)) {
			return playerBSession;
		}
		if (playerBSession.equals(sessionId)) {
			return playerASession;
		}
		return null;
	}

	/** 五子棋：A 执黑；象棋：A 执红 */
	public boolean isSideA(String sessionId) {
		return playerASession.equals(sessionId);
	}
}
