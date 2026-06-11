package game.manager.table;

import game.manager.table.card.mj.MjTilePool;
import game.manager.table.mj.*;
import game.manager.table.replay.MjReplayRecorder;
import game.manager.table.replay.ReplayRecorder;
import model.tablemodel.TableModel;
import msg.registor.enums.TableState;
import msg.registor.message.GMsg;
import net.client.Sender;
import proto.ConstProto;
import proto.GameProto;
import proto.ModelProto;

import java.util.*;

/**
 * 麻将桌子
 * 包含麻将特有的牌墙、上下文、赖子、副露等
 */
public class MjTable extends Table {

	private final MjTilePool mjTilePool;
	private final MjTableContext mjContext = new MjTableContext();

	public MjTable(long tableId, TableModel model, ModelProto.RoomRole creator) {
		super(tableId, model, creator);
		this.mjTilePool = new MjTilePool(this);
	}

	// ======================== 抽象方法实现 ========================

	@Override
	public int getGameType() { return 1; }

	@Override
	public void dealCards() {
		mjTilePool.dealInitTiles();
	}

	@Override
	public void resetGameContext() {
		mjContext.resetRound();
	}

	@Override
	public GameResult createGameResult() {
		MjGameResult result = new MjGameResult();
		result.setTotalRounds(getTableModel().getTotalRounds());
		return result;
	}

	@Override
	public ReplayRecorder createReplayRecorder() {
		return new MjReplayRecorder(getTableId(), getCurrentRound());
	}

	@Override
	public void initGameConfig() {
		int subType = getTableModel().getGameSubType();
		if (subType == 2) { // 卡五星
			mjTilePool.setAllowedSuits(new int[]{1, 2});
		}
	}

	@Override
	public int processOp(int userId, GameProto.OpInfo op, Sender sender, long mapId, int sequence) {
		TableState ts = getTableState();

		if (ts == TableState.MJ_DISCARD) {
			return processMjDiscard(userId, op);
		}
		if (ts == TableState.MJ_CLAIM) {
			return processMjClaim(userId, op);
		}

		return ConstProto.Result.OP_CURR_ERROR_VALUE;
	}

	@Override
	public void syncGameState(TableUser user) {
		int seat = user.getSeated();
		if (seat < 0) return;

		// 1. 同步手牌
		mjTilePool.sendHandNotice(getSeatUsers());

		// 2. 同步副露区
		int seatNum = getTableModel().getSeatNum();
		for (int i = 0; i < seatNum; i++) {
			List<MjExposedSet> sets = mjContext.getExposedSets(i);
			for (MjExposedSet set : sets) {
				GameProto.NotMjState.Builder notBuilder = GameProto.NotMjState.newBuilder()
						.setOpSeat(i).setAction(ConstProto.Operation.MJ_PASS)
						.setWallLeft(mjTilePool.remaining());
				switch (set.getType()) {
					case PENG: notBuilder.setTileId(set.getTileIds().get(0)); notBuilder.setAction(ConstProto.Operation.MJ_PENG); break;
					case MING_GANG: case AN_GANG: case BU_GANG: notBuilder.setTileId(set.getGangTileId()); notBuilder.setAction(ConstProto.Operation.MJ_GANG); break;
					case CHI: notBuilder.setTileId(set.getTileIds().get(0)); notBuilder.setAction(ConstProto.Operation.MJ_CHI); break;
				}
				user.sendRoleMessage(notBuilder.build(), GMsg.MJ_TILE_NOT, getTableId());
			}
		}

		// 3. 同步状态
		GameProto.NotTableState stateNot = GameProto.NotTableState.newBuilder()
				.setState(getTableState().getId()).setStateStart(getStateStartTime())
				.setStateDuration(getTableState().getOverTime()).build();
		user.sendRoleMessage(stateNot, GMsg.NOT_TABLE_STATE, getTableId());

		// 4. 同步赖子
		if (getTableModel().getGameSubType() == 1 && mjContext.getLaiZiTileId() != 0) {
			GameProto.NotMjState laiZiNot = GameProto.NotMjState.newBuilder()
					.setOpSeat(-1).setTileId(mjContext.getLaiZiFlipTile())
					.setAction(ConstProto.Operation.DRAW).setWallLeft(mjTilePool.remaining()).build();
			user.sendRoleMessage(laiZiNot, GMsg.MJ_TILE_NOT, getTableId());
		}
	}

	// ======================== MJ内部方法 ========================

	private int processMjDiscard(int userId, GameProto.OpInfo op) {
		ConstProto.Operation choice = op.getChoice();
		if (choice == ConstProto.Operation.MJ_GANG) {
			if (op.getOpCardsCount() > 0) {
				int gangTileId = op.getOpCards(0).getCards(0).getValue();
				if (MjPlayService.applyAnGang(this, gangTileId)) return ConstProto.Result.SUCCESS_VALUE;
				if (MjPlayService.applyBuGang(this, gangTileId)) return ConstProto.Result.SUCCESS_VALUE;
			}
			return ConstProto.Result.OP_CURR_ERROR_VALUE;
		}
		boolean ok = MjPlayService.applyDiscard(this, userId, op);
		if (!ok) return ConstProto.Result.OP_CURR_ERROR_VALUE;
		if (!MjPlayService.checkClaim(this)) {
			MjPlayService.nextPlayer(this);
			long now = System.currentTimeMillis();
			upNextStateWithTime(TableState.MJ_PLAY, now);
		}
		return ConstProto.Result.SUCCESS_VALUE;
	}

	private int processMjClaim(int userId, GameProto.OpInfo op) {
		boolean ok = MjPlayService.applyClaim(this, userId, op);
		return ok ? ConstProto.Result.SUCCESS_VALUE : ConstProto.Result.OP_CURR_ERROR_VALUE;
	}

	// ======================== MJ特有getter ========================

	public MjTilePool getMjTilePool() { return mjTilePool; }
	public MjTableContext getMjContext() { return mjContext; }
}
