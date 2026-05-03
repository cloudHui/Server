package game.manager.table.banner;

import java.util.ArrayList;
import java.util.List;

/**
 * @author cloud
 * @date 2026-05-03
 * @version 1.0
 * @since 1.0
 * @className Banner
 * @description 斗地主叫分与抢地主阶段状态，负责游戏桌子的叫分与抢地主阶段状态管理
 * @createDate 2026-05-03
 */
public class Banner {

	private int firstRandomRobSeat = -1;
	private int firstRobSeat = -1;

	private boolean robBroadcastDone;

	/** false=叫分阶段 true=抢地主阶段（各农民各决策一次） */
	private boolean robPhase;

	private int maxCallScore;//最大叫分
	private int candidateSeat = -1;//候选人座位

	private int bidResponses;//叫分响应次数

	private final List<Integer> robFarmerSeats = new ArrayList<>();//抢地主座位列表
	private int robTurnIndex;//抢地主轮次
	private int robResponses;//抢地主响应次数

	/** 抢地主累积倍数，初始 1，每次「抢」×2 */
	private int robMultiplierAccum = 1;//抢地主累积倍数

	public Banner() {
	}

	public int getFirstRandomRobSeat() {
		return firstRandomRobSeat;
	}

	public void setFirstRandomRobSeat(int firstRandomRobSeat) {
		this.firstRandomRobSeat = firstRandomRobSeat;
	}

	public int getFirstRobSeat() {
		return firstRobSeat;
	}

	public void setFirstRobSeat(int firstRobSeat) {
		this.firstRobSeat = firstRobSeat;
	}

	public boolean isRobBroadcastDone() {
		return robBroadcastDone;
	}

	public void setRobBroadcastDone(boolean robBroadcastDone) {
		this.robBroadcastDone = robBroadcastDone;
	}

	public boolean isRobPhase() {
		return robPhase;
	}

	public void setRobPhase(boolean robPhase) {
		this.robPhase = robPhase;
	}

	public int getMaxCallScore() {
		return maxCallScore;
	}

	public void setMaxCallScore(int maxCallScore) {
		this.maxCallScore = maxCallScore;
	}

	public int getCandidateSeat() {
		return candidateSeat;
	}

	public void setCandidateSeat(int candidateSeat) {
		this.candidateSeat = candidateSeat;
	}

	public int getBidResponses() {
		return bidResponses;
	}

	public void setBidResponses(int bidResponses) {
		this.bidResponses = bidResponses;
	}

	public void addBidResponse() {
		this.bidResponses++;
	}

	public List<Integer> getRobFarmerSeats() {
		return robFarmerSeats;
	}

	public int getRobTurnIndex() {
		return robTurnIndex;
	}

	public void setRobTurnIndex(int robTurnIndex) {
		this.robTurnIndex = robTurnIndex;
	}

	public int getRobResponses() {
		return robResponses;
	}

	public void setRobResponses(int robResponses) {
		this.robResponses = robResponses;
	}

	public void addRobResponse() {
		this.robResponses++;
	}

	public int getRobMultiplierAccum() {
		return robMultiplierAccum;
	}

	public void setRobMultiplierAccum(int robMultiplierAccum) {
		this.robMultiplierAccum = robMultiplierAccum;
	}

	/**
	 * 抢地主阶段当前应操作的座位（农民）。
	 */
	public int getCurrentRobSeat() {
		if (robTurnIndex < 0 || robTurnIndex >= robFarmerSeats.size()) {
			return -1;
		}
		return robFarmerSeats.get(robTurnIndex);
	}

	/**
	 * 进入抢地主阶段：按座位顺序两名农民各决策一次。
	 */
	public void prepareRobFarmerOrder(int candidateSeat, int seatNum) {
		robFarmerSeats.clear();
		for (int step = 1; step < seatNum; step++) {
			robFarmerSeats.add((candidateSeat + step) % seatNum);
		}
		robTurnIndex = 0;
		robResponses = 0;
		robMultiplierAccum = 1;
	}

	/**
	 * 抢地主轮次递增
	 */
	public void advanceRobTurn() {
		robTurnIndex++;
	}

	/**
	 * 重置抢地主阶段状态
	 */
	public void reset() {
		firstRandomRobSeat = -1;
		firstRobSeat = -1;
		robBroadcastDone = false;
		robPhase = false;
		maxCallScore = 0;
		candidateSeat = -1;
		bidResponses = 0;
		robFarmerSeats.clear();
		robTurnIndex = 0;
		robResponses = 0;
		robMultiplierAccum = 1;
	}
}
