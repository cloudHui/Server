package game.manager.table.ddz;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import game.manager.table.cards.Card;
import game.manager.table.ddz.ai.AiVision;
import proto.GameProto;

/**
 * 斗地主一桌内的运行时状态（与 {@link msg.registor.enums.TableState} 区分）。
 * 
 * @author cloud
 * @date 2026-05-03
 * @version 1.0
 * @since 1.0
 * @className DdzTableContext
 * @description 斗地主一桌内的运行时状态（与 {@link msg.registor.enums.TableState} 区分）。
 * @createDate 2026-05-03
 */
public class DdzTableContext {

	private GameProto.CardInfo lastPlayed = GameProto.CardInfo.getDefaultInstance();
	private DdzHand lastHand;//上一手出牌
	private int consecutivePasses;//连续过牌次数
	private int landlordSeat = -1;
	/** 上一手出牌者座位（用于两轮「要不起」后仍由其首家） */
	private int lastPlaySeat = -1;//上一手出牌者座位

	/** 叫分底分 1-3 */
	private int baseScore = 1;//叫分底分
	/** 抢地主倍数（每抢×2 后的积） */
	private int robMultiplier = 1;//抢地主倍数
	private boolean farmerEverPlayed;//农民是否出过牌
	private int landlordPlayCount;//地主出牌次数
	/** 记牌器：本局所有已出牌的 cardId 集合 */
	private final Set<Integer> playedCardIds = new HashSet<>();
	/** AI 视野等级：0=正常, 1=半透视(知剩余牌池), 2=全透视(知他人手牌)。支持运行时修改 */
	private int visionLevel = AiVision.LEVEL_NORMAL;
	/** AI 智能等级：0=最笨(出最小/pass), 1=基础策略, 2=高级策略。支持运行时修改 */
	private int aiLevel = AiVision.AI_ADVANCED;

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

	/** 记录一批已出牌的 cardId */
	public void recordPlayedCards(List<Card> cards) {
		for (Card c : cards) {
			playedCardIds.add(c.getId());
		}
	}

	/** 获取所有已出牌 ID 集合（只读） */
	public Set<Integer> getPlayedCardIds() {
		return playedCardIds;
	}

	public int getVisionLevel() {
		return visionLevel;
	}

	/** 运行时修改 AI 视野等级（GM/管理台调用） */
	public void setVisionLevel(int visionLevel) {
		this.visionLevel = visionLevel;
	}

	public int getAiLevel() {
		return aiLevel;
	}

	/** 运行时修改 AI 智能等级（GM/管理台调用） */
	public void setAiLevel(int aiLevel) {
		this.aiLevel = aiLevel;
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
		playedCardIds.clear();
		resetCurrentTrickCards();
	}
}
