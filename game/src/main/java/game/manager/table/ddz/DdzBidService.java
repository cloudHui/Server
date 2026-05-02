package game.manager.table.ddz;

import java.util.concurrent.ThreadLocalRandom;

import game.manager.table.Table;
import game.manager.table.TableUser;
import game.manager.table.banner.Banner;
import msg.registor.enums.TableState;
import msg.registor.message.GMsg;
import proto.ConstProto;
import proto.GameProto;

/**
 * 叫分 1/2/3（每人一轮）→ 两名农民各一轮抢地主（每次「抢」倍数×2），再确定地主并入桌出牌。
 */
public final class DdzBidService {

	private DdzBidService() {
	}

	public static void onBidTimeout(Table table) {
		int seat = table.getOp().getCurrOpSeat();
		TableUser u = table.getSeatUser(seat);
		if (u == null) {
			return;
		}
		Banner banner = table.getBanner();
		if (!banner.isRobPhase()) {
			apply(table, u.getUserId(), GameProto.OpInfo.newBuilder()
					.setChoice(ConstProto.Operation.NOT_CALL).build());
		} else {
			apply(table, u.getUserId(), GameProto.OpInfo.newBuilder()
					.setChoice(ConstProto.Operation.NOT_ROB).build());
		}
	}

	public static int apply(Table table, int userId, GameProto.OpInfo opInfo) {
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

	private static int applyCall(Table table, int userId, GameProto.OpInfo opInfo, TableUser user, Banner banner) {
		int cv = opInfo.getChoiceValue();
		if (cv != ConstProto.Operation.NOT_CALL_VALUE && !DdzBidOpcodes.isCallScore(cv)) {
			return ConstProto.Result.OP_CURR_ERROR_VALUE;
		}
		int score = cv == ConstProto.Operation.NOT_CALL_VALUE ? 0 : DdzBidOpcodes.callScoreFromChoiceValue(cv);
		if (score > banner.getMaxCallScore()) {
			banner.setMaxCallScore(score);
			banner.setCandidateSeat(user.getSeated());
		} else if (score > 0 && score == banner.getMaxCallScore()) {
			banner.setCandidateSeat(user.getSeated());
		}
		broadcastAck(table, userId, GameProto.OpInfo.newBuilder().setChoiceValue(cv).build());
		banner.addBidResponse();
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

	private static void completeCallPhase(Table table, Banner banner, int seatNum) {
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

	private static int applyRob(Table table, int userId, GameProto.OpInfo opInfo, TableUser user, Banner banner) {
		int cv = opInfo.getChoiceValue();
		if (cv != ConstProto.Operation.ROB_VALUE && cv != ConstProto.Operation.NOT_ROB_VALUE) {
			return ConstProto.Result.OP_CURR_ERROR_VALUE;
		}
		if (user.getSeated() != banner.getCurrentRobSeat()) {
			return ConstProto.Result.OP_CURR_ERROR_VALUE;
		}
		if (cv == ConstProto.Operation.ROB_VALUE) {
			banner.setRobMultiplierAccum(banner.getRobMultiplierAccum() * 2);
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

	private static void finishBidding(Table table, Banner banner) {
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
		table.getOp().reset();
		table.getOp().setCurrOpSeat(landlordSeat);
		table.upNextStateWithTime(TableState.CARD, System.currentTimeMillis());
	}

	private static void broadcastAck(Table table, int actorUserId, GameProto.OpInfo op) {
		GameProto.AckOp msg = GameProto.AckOp.newBuilder()
				.setOp(op)
				.setOpId(actorUserId)
				.setOpFrom(actorUserId)
				.build();
		table.sendTableMessage(msg, GMsg.ACK_OP);
	}
}
