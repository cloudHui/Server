package game.manager.table.state;

import game.Game;
import game.manager.table.Table;
import game.manager.table.TableUser;
import game.manager.table.mj.MjDrawService;
import game.manager.table.replay.ReplayRecorder;
import model.tablemodel.TableModel;
import msg.annotation.ProcessEnum;
import msg.registor.enums.TableState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * 等待阶段：坐满后开局；支持等人超时解散/补机器人；全机器人可删桌不开局。
 */
@ProcessEnum(TableState.WAITING)
public class Waiting extends AbstractTableHandle {
	private static final Logger logger = LoggerFactory.getLogger(Waiting.class);

	@Override
	public boolean onTiming(Table table) {
		if (table.sitFull()) {
			if (table.isAllRobot()) {
				logger.info("全机器人不开局，解散桌子, tableId: {}", table.getTableId());
				table.upNextState(TableState.TABLE_DIS);
				return false;
			}
			startGame(table);
			return false;
		}
		if (table.isEmpty() || !table.hasHumanPlayer()) {
			logger.info("等待阶段无真人，解散桌子, tableId: {}, empty: {}, allRobot: {}",
					table.getTableId(), table.isEmpty(), table.isAllRobot());
			Game.getInstance().getTableManager().removeTable(table.getTableId());
			return true;
		}

		TableModel model = table.getTableModel();
		int waitSec = model.getWaitTimeoutSec();
		if (waitSec > 0) {
			long deadline = table.getStateStartTime() + waitSec * 1000L;
			if (System.currentTimeMillis() >= deadline) {
				if (model.getWaitTimeoutAction() == 1) {
					logger.info("等人超时，补机器人, tableId: {}, waitSec: {}", table.getTableId(), waitSec);
					int added = table.fillRobotSeats();
					if (added <= 0 && !table.sitFull()) {
						logger.warn("补机器人失败，回退解散, tableId: {}", table.getTableId());
						table.upNextState(TableState.TABLE_DIS);
					} else if (table.sitFull()) {
						if (table.isAllRobot()) {
							logger.info("补机器人后全机桌，解散, tableId: {}", table.getTableId());
							table.upNextState(TableState.TABLE_DIS);
						} else {
							startGame(table);
						}
					} else {
						// 未坐满：重置计时，避免每个 tick 反复补位
						table.upNextStateWithTime(TableState.WAITING, System.currentTimeMillis());
					}
				} else {
					logger.info("等人超时，解散桌子, tableId: {}, waitSec: {}", table.getTableId(), waitSec);
					table.upNextState(TableState.TABLE_DIS);
				}
			}
		}
		return false;
	}

	private void startGame(Table table) {
		table.initGameConfig();
		table.dealCards();
		table.getOp().setCurrOpSeat(0);

		initReplay(table);
		recordInitHands(table);

		if (table.getGameType() == 1) {
			if (table.getTableModel().getGameSubType() == 1) {
				MjDrawService.flipLaiZi(game.manager.table.MjTable.class.cast(table));
			}
			table.upNextState(TableState.MJ_DEAL);
		} else {
			table.upNextState();
		}
	}

	private void initReplay(Table table) {
		ReplayRecorder replay = table.createReplayRecorder();
		if (replay == null) return;

		table.setReplayRecorder(replay);

		Map<Integer, Integer> userIds = new HashMap<>();
		Map<Integer, String> nicknames = new HashMap<>();
		for (Map.Entry<Integer, TableUser> entry : table.getSeatUsers().entrySet()) {
			userIds.put(entry.getKey(), entry.getValue().getUserId());
			nicknames.put(entry.getKey(), entry.getValue().getNick());
		}

		String gameType;
		if (table.getGameType() == 1) {
			switch (table.getTableModel().getGameSubType()) {
				case 1: gameType = "荆门麻将"; break;
				case 2: gameType = "卡五星"; break;
				default: gameType = "麻将"; break;
			}
		} else {
			gameType = "斗地主";
		}

		replay.writeHeader(gameType, table.getTableModel().getTotalRounds(),
				table.getTableModel().getSeatNum(), userIds, nicknames);
		replay.writeConfig("底分=" + table.getTableModel().getBaseScore()
				+ ", 最大番=" + table.getTableModel().getMaxFan()
				+ ", autoPlay=" + table.getTableModel().getAutoPlay()
				+ ", waitTimeoutSec=" + table.getTableModel().getWaitTimeoutSec()
				+ ", waitTimeoutAction=" + table.getTableModel().getWaitTimeoutAction());

		if (table.getGameType() == 1) {
			game.manager.table.MjTable mjTable = (game.manager.table.MjTable) table;
			replay.writeDealerAndLaiZi(mjTable.getMjContext().getDealerSeat(),
					mjTable.getMjContext().getLaiZiTileId(),
					mjTable.getMjContext().getLaiZiFlipTile());
		}
	}

	private void recordInitHands(Table table) {
		ReplayRecorder replay = table.getReplayRecorder();
		if (replay == null) return;

		Map<Integer, List<Integer>> hands = new HashMap<>();
		for (Map.Entry<Integer, TableUser> entry : table.getSeatUsers().entrySet()) {
			List<Integer> tileIds = new ArrayList<>();
			for (game.manager.table.cards.Card c : entry.getValue().getCards()) {
				tileIds.add(c.getId());
			}
			hands.put(entry.getKey(), tileIds);
		}
		replay.writeInitHands(hands);
	}
}
