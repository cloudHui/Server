package game.manager.table.state;

import game.Game;
import game.manager.table.Table;
import game.manager.table.TableUser;
import game.manager.table.mj.MjDrawService;
import game.manager.table.replay.ReplayRecorder;
import msg.annotation.ProcessEnum;
import msg.registor.enums.TableState;

import java.util.*;

/**
 * 等待阶段：坐满后调用 table.initGameConfig() + dealCards()，由子类各自实现
 */
@ProcessEnum(TableState.WAITING)
public class Waiting extends AbstractTableHandle {

	@Override
	public boolean onTiming(Table table) {
		if (table.sitFull()) {
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
			return false;
		}
		if (table.isEmpty()) {
			Game.getInstance().getTableManager().removeTable(table.getTableId());
			return true;
		}
		return false;
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
				+ ", autoPlay=" + table.getTableModel().getAutoPlay());

		if (table.getGameType() == 1) {
			game.manager.table.MjTable mjTable = game.manager.table.MjTable.class.cast(table);
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
