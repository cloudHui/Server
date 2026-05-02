package game.manager.table.ddz;

import proto.GameProto;

/**
 * 斗地主一桌内的运行时状态（与 {@link msg.registor.enums.TableState} 区分）。
 */
public class DdzTableContext {

	private GameProto.CardInfo lastPlayed = GameProto.CardInfo.getDefaultInstance();
	private DdzHand lastHand;
	private int consecutivePasses;
	private int landlordSeat = -1;
	/** 上一手出牌者座位（用于两轮「要不起」后仍由其首家） */
	private int lastPlaySeat = -1;

	/** 叫分底分 1-3 */
	private int baseScore = 1;
	/** 抢地主倍数（每抢×2 后的积） */
	private int robMultiplier = 1;
	private boolean farmerEverPlayed;
	private int landlordPlayCount;

	public GameProto.CardInfo getLastPlayed() {
		return lastPlayed;
	}

	public void setLastPlayed(GameProto.CardInfo lastPlayed) {
		this.lastPlayed = lastPlayed != null ? lastPlayed : GameProto.CardInfo.getDefaultInstance();
	}

	public DdzHand getLastHand() {
		return lastHand;
	}

	public void setLastHand(DdzHand lastHand) {
		this.lastHand = lastHand;
	}

	public int getConsecutivePasses() {
		return consecutivePasses;
	}

	public void setConsecutivePasses(int consecutivePasses) {
		this.consecutivePasses = consecutivePasses;
	}

	public void addPass() {
		consecutivePasses++;
	}

	public int getLandlordSeat() {
		return landlordSeat;
	}

	public void setLandlordSeat(int landlordSeat) {
		this.landlordSeat = landlordSeat;
	}

	public int getLastPlaySeat() {
		return lastPlaySeat;
	}

	public void setLastPlaySeat(int lastPlaySeat) {
		this.lastPlaySeat = lastPlaySeat;
	}

	public int getBaseScore() {
		return baseScore;
	}

	public void setBaseScore(int baseScore) {
		this.baseScore = Math.max(1, baseScore);
	}

	public int getRobMultiplier() {
		return robMultiplier;
	}

	public void setRobMultiplier(int robMultiplier) {
		this.robMultiplier = Math.max(1, robMultiplier);
	}

	public boolean isFarmerEverPlayed() {
		return farmerEverPlayed;
	}

	public void setFarmerEverPlayed(boolean farmerEverPlayed) {
		this.farmerEverPlayed = farmerEverPlayed;
	}

	public int getLandlordPlayCount() {
		return landlordPlayCount;
	}

	public void incrementLandlordPlayCount() {
		this.landlordPlayCount++;
	}

	public void setLandlordPlayCount(int landlordPlayCount) {
		this.landlordPlayCount = landlordPlayCount;
	}

	public void resetCurrentTrickCards() {
		lastPlayed = GameProto.CardInfo.getDefaultInstance();
		lastHand = null;
		consecutivePasses = 0;
	}

	public void resetHand() {
		landlordSeat = -1;
		lastPlaySeat = -1;
		baseScore = 1;
		robMultiplier = 1;
		farmerEverPlayed = false;
		landlordPlayCount = 0;
		resetCurrentTrickCards();
	}
}
