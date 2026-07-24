package game.manager.table.ddz;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import game.manager.table.DdzTable;
import game.manager.table.TableUser;
import game.manager.table.banner.Banner;
import game.manager.table.replay.DdzReplayRecorder;
import game.manager.table.replay.ReplayRecorder;
import msg.registor.enums.TableState;
import msg.registor.message.GMsg;
import proto.ConstProto;
import proto.GameProto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 叫分 1/2/3（每人一轮）→ 两名农民各一轮抢地主（每次「抢」倍数×2），再确定地主并入桌出牌。
 * 
 * @author cloud
 * @date 2026-05-03
 * @version 1.0
 * @since 1.0
 */
public final class DdzBidService {

	private static final Logger logger = LoggerFactory.getLogger(DdzBidService.class);

	private DdzBidService() {
	}

	/**
	 * 叫分超时，自动视为不叫/不抢
	 * 
	 * @param table 桌子
	 */
	public static void onBidTimeout(DdzTable table) {
		int seat = table.getOp().getCurrOpSeat();
		TableUser u = table.getSeatUser(seat);
		if (u == null) {
			return;
		}
		Banner banner = table.getBanner();
		logger.info("叫分超时自动处理, tableId: {}, seat: {}, robPhase: {}",
				table.getTableId(), seat, banner.isRobPhase());
		if (!banner.isRobPhase()) {
			apply(table, u.getUserId(), GameProto.OpInfo.newBuilder()
					.setChoice(ConstProto.Operation.NOT_CALL).build());
		} else {
			apply(table, u.getUserId(), GameProto.OpInfo.newBuilder()
					.setChoice(ConstProto.Operation.NOT_ROB).build());
		}
	}

	/**
	 * 应用叫分
	 * 
	 * @param table  桌子
	 * @param userId 用户ID
	 * @param opInfo 操作信息
	 * @return 结果
	 */
	public static int apply(DdzTable table, int userId, GameProto.OpInfo opInfo) {
		if (table.getTableState() != TableState.IDLE_ROB) {
			return ConstProto.Result.OP_CURR_ERROR_VALUE;
		}
		TableUser user = table.getUsers().get(userId);
		if (user == null || user.getSeated() != table.getOp().getCurrOpSeat()) {
			return ConstProto.Result.OP_CURR_ERROR_VALUE;
		}
		Banner banner = table.getBanner();
		if (!banner.isRobPhase()) {
			return applyCall(table, userId, opInfo, user, banner);
		}
		return applyRob(table, userId, opInfo, user, banner);
	}

	/**
	 * 应用叫分
	 * 
	 * @param table  桌子
	 * @param userId 用户ID
	 * @param opInfo 操作信息
	 * @param user   用户
	 * @param banner 横幅
	 * @return 结果
	 */
	private static int applyCall(DdzTable table, int userId, GameProto.OpInfo opInfo, TableUser user, Banner banner) {
		int cv = opInfo.getChoiceValue();
		if (table.getTableModel().getGameSubType() == 1) {
			if (cv != ConstProto.Operation.CALL_VALUE && cv != ConstProto.Operation.NOT_CALL_VALUE) return ConstProto.Result.OP_CURR_ERROR_VALUE;
			if (cv == ConstProto.Operation.NOT_CALL_VALUE) {
				broadcastAck(table, userId, GameProto.OpInfo.newBuilder().setChoiceValue(cv).build());
				banner.setRobPhase(false);
				int next = (user.getSeated() + 1) % table.getTableModel().getSeatNum();
				banner.setFirstRandomRobSeat(next); table.getOp().setCurrOpSeat(next); banner.setRobBroadcastDone(false);
				table.upNextStateWithTime(TableState.ROB, System.currentTimeMillis());
				return ConstProto.Result.SUCCESS_VALUE;
			}
			banner.setCandidateSeat(user.getSeated()); banner.setMaxCallScore(1);
			banner.setRobPhase(true); banner.prepareRobFarmerOrder(user.getSeated(), table.getTableModel().getSeatNum());
			banner.setRobBroadcastDone(false); table.getOp().setCurrOpSeat(banner.getCurrentRobSeat());
			broadcastAck(table, userId, GameProto.OpInfo.newBuilder().setChoiceValue(cv).build());
			table.upNextStateWithTime(TableState.ROB, System.currentTimeMillis());
			return ConstProto.Result.SUCCESS_VALUE;
		}
		if (cv != ConstProto.Operation.NOT_CALL_VALUE && !DdzBidOpcodes.isCallScore(cv)) {
			return ConstProto.Result.OP_CURR_ERROR_VALUE;
		}
		int score = cv == ConstProto.Operation.NOT_CALL_VALUE ? 0 : DdzBidOpcodes.callScoreFromChoiceValue(cv);
		if (score > 0 && !banner.isScoreAvailable(score)) {
			return ConstProto.Result.OP_CURR_ERROR_VALUE;
		}
		banner.addCalledScore(score);

		ReplayRecorder replay = table.getReplayRecorder();
		if (replay instanceof DdzReplayRecorder) {
			if (score > 0) {
				((DdzReplayRecorder) replay).recordBid(user.getSeated(), score);
			} else {
				((DdzReplayRecorder) replay).recordNotCall(user.getSeated());
			}
		}

		if (score > banner.getMaxCallScore()) {
			banner.setMaxCallScore(score);
			banner.setCandidateSeat(user.getSeated());
		}
		broadcastAck(table, userId, GameProto.OpInfo.newBuilder().setChoiceValue(cv).build());
		banner.addBidResponse();
		// 叫到最高分直接定地主，不再进入抢地主阶段。
		if (score == 3) {
			banner.setCandidateSeat(user.getSeated());
			banner.setMaxCallScore(3);
			finishBidding(table, banner);
			return ConstProto.Result.SUCCESS_VALUE;
		}
		int seatNum = table.getTableModel().getSeatNum();
		if (banner.getBidResponses() >= seatNum) {
			completeCallPhase(table, banner, seatNum);
		} else {
			table.getOp().moveToNextOp();
			banner.setRobBroadcastDone(false);
			table.upNextStateWithTime(TableState.ROB, System.currentTimeMillis());
		}
		return ConstProto.Result.SUCCESS_VALUE;
	}

	/**
	 * 完成叫分阶段
	 * 
	 * @param table   桌子
	 * @param banner  横幅
	 * @param seatNum 座位数
	 */
	private static void completeCallPhase(DdzTable table, Banner banner, int seatNum) {
		if (banner.getMaxCallScore() <= 0) {
			banner.setCandidateSeat(ThreadLocalRandom.current().nextInt(seatNum));
			banner.setMaxCallScore(1);
		}
		banner.setRobPhase(true);
		banner.prepareRobFarmerOrder(banner.getCandidateSeat(), seatNum);
		banner.setRobBroadcastDone(false);
		table.getOp().setCurrOpSeat(banner.getCurrentRobSeat());
		table.upNextStateWithTime(TableState.ROB, System.currentTimeMillis());
	}

	/**
	 * 应用抢地主
	 * 
	 * @param table  桌子
	 * @param userId 用户ID
	 * @param opInfo 操作信息
	 * @param user   用户
	 * @param banner 横幅
	 * @return 结果
	 */
	private static int applyRob(DdzTable table, int userId, GameProto.OpInfo opInfo, TableUser user, Banner banner) {
		int cv = opInfo.getChoiceValue();
		if (cv != ConstProto.Operation.ROB_VALUE && cv != ConstProto.Operation.NOT_ROB_VALUE) {
			return ConstProto.Result.OP_CURR_ERROR_VALUE;
		}
		if (user.getSeated() != banner.getCurrentRobSeat()) {
			return ConstProto.Result.OP_CURR_ERROR_VALUE;
		}

		ReplayRecorder replay = table.getReplayRecorder();
		if (replay instanceof DdzReplayRecorder) {
			if (cv == ConstProto.Operation.ROB_VALUE) {
				((DdzReplayRecorder) replay).recordRob(user.getSeated());
			} else {
				((DdzReplayRecorder) replay).recordNotRob(user.getSeated());
			}
		}

		if (cv == ConstProto.Operation.ROB_VALUE) {
			banner.setRobMultiplierAccum(banner.getRobMultiplierAccum() * 2);
			// 抢/再抢成功者成为当前地主候选人，后续无人再抢时由其当选。
			banner.setCandidateSeat(user.getSeated());
		}
		broadcastAck(table, userId, GameProto.OpInfo.newBuilder().setChoiceValue(cv).build());
		banner.addRobResponse();
		if (banner.getRobResponses() >= banner.getRobFarmerSeats().size()) {
			finishBidding(table, banner);
		} else {
			banner.advanceRobTurn();
			table.getOp().setCurrOpSeat(banner.getCurrentRobSeat());
			banner.setRobBroadcastDone(false);
			table.upNextStateWithTime(TableState.ROB, System.currentTimeMillis());
		}
		return ConstProto.Result.SUCCESS_VALUE;
	}

	/**
	 * 完成叫分
	 * 
	 * @param table  桌子
	 * @param banner 横幅
	 */
	private static void finishBidding(DdzTable table, Banner banner) {
		int landlordSeat = banner.getCandidateSeat();
		int baseScore = Math.max(1, banner.getMaxCallScore());
		table.getDdz().setLandlordSeat(landlordSeat);
		table.getDdz().setBaseScore(baseScore);
		table.getDdz().setRobMultiplier(banner.getRobMultiplierAccum());
		table.getDdz().resetCurrentTrickCards();
		table.getDdz().setLastPlaySeat(-1);
		table.getDdz().setFarmerEverPlayed(false);
		table.getDdz().setLandlordPlayCount(0);
		table.getCardPool().attachBottomToLandlord(table, landlordSeat);

		ReplayRecorder replay = table.getReplayRecorder();
		if (replay instanceof DdzReplayRecorder) {
			List<Integer> bottomIds = new ArrayList<>(table.getDdz().getRevealedBottomCards());
			((DdzReplayRecorder) replay).recordBottomCards(landlordSeat, bottomIds);
		}

		table.getOp().reset();
		table.getOp().setCurrOpSeat(landlordSeat);
		table.upNextStateWithTime(TableState.CARD, System.currentTimeMillis());
	}

	/**
	 * 广播确认
	 * 
	 * @param table       桌子
	 * @param actorUserId 用户ID
	 * @param op          操作信息
	 */
	private static void broadcastAck(DdzTable table, int actorUserId, GameProto.OpInfo op) {
		GameProto.AckOp msg = GameProto.AckOp.newBuilder()
				.setOp(op)
				.setOpId(actorUserId)
				.setOpFrom(actorUserId)
				.build();
		table.sendTableMessage(msg, GMsg.ACK_OP);
	}
}
