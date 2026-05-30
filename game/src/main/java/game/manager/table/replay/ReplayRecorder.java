package game.manager.table.replay;

import game.manager.table.mj.MjExposedSet;

import java.util.List;
import java.util.Map;

/**
 * 回放记录器接口
 * 通用框架, 各玩法实现自己的格式
 */
public interface ReplayRecorder {

	// ======================== 头部信息 ========================
	void writeHeader(String gameType, int totalRounds, int seatNum, Map<Integer, Integer> userIds, Map<Integer, String> nicknames);
	void writeConfig(String configLine);
	void writeDealerAndLaiZi(int dealerSeat, int laiZiTileId, int flipTileId);

	// ======================== 初始发牌 ========================
	void writeInitHands(Map<Integer, List<Integer>> hands);

	// ======================== Delta事件(MJ) ========================
	void recordDraw(int seat, int tileId);
	void recordDiscard(int seat, int tileId);
	void recordAutoDiscard(int seat, int tileId);
	void recordChi(int seat, int fromSeat, List<Integer> chiTiles, List<Integer> handTiles);
	void recordPeng(int seat, int fromSeat, int tileId);
	void recordMingGang(int seat, int fromSeat, int tileId);
	void recordAnGang(int seat, int tileId, int drawnTile);
	void recordBuGang(int seat, int tileId, int drawnTile);
	void recordBuGangRobbed(int seat, int tileId, int robberSeat);
	void recordHu(int seat, int tileId, boolean ziMo);
	void recordAutoPass(int seat);
	void recordDrawGame();

	// ======================== 最终状态 ========================
	void writeFinalState(Map<Integer, List<Integer>> finalHands, Map<Integer, List<MjExposedSet>> exposedSetsMap, int wallRemaining);

	// ======================== 结算 ========================
	void writeSettlement(int winnerSeat, int fan, String winType, int[] scores);

	// ======================== 保存 ========================
	void save();
}
