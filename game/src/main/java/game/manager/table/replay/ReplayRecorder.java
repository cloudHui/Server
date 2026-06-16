package game.manager.table.replay;

import game.manager.table.mj.MjExposedSet;

import java.util.List;
import java.util.Map;

public interface ReplayRecorder {

	// ======================== 头部信息 ========================
	void writeHeader(String gameType, int totalRounds, int seatNum, Map<Integer, Integer> userIds, Map<Integer, String> nicknames);
	void writeConfig(String configLine);
	void writeDealerAndLaiZi(int dealerSeat, int laiZiTileId, int flipTileId);

	// ======================== 初始发牌 ========================
	void writeInitHands(Map<Integer, List<Integer>> hands);

	// ======================== 结算 ========================
	void writeSettlement(int winnerSeat, int fan, String winType, int[] scores);

	// ======================== 保存 ========================
	void save();
}
