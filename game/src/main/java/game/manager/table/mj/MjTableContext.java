package game.manager.table.mj;

import java.util.*;

/**
 * 麻将一桌内的运行时状态
 */
public class MjTableContext {

	/** 当前庄家座位 */
	private int dealerSeat;

	/** 当前摸的牌ID(用于超时自动打牌) */
	private int drawnTile;

	/** 当前玩家已摸牌(防止重复摸牌) */
	private boolean tileDrawn;

	/** 出牌提示已发送(防止重复发送) */
	private boolean discardPromptSent;

	/** 最后打出的牌ID */
	private int lastDiscardTile;

	/** 最后出牌的座位 */
	private int lastDiscardSeat;

	// --- 新增字段 ---

	/** 每个座位的副露区(碰/杠/吃亮出的牌) */
	private final Map<Integer, List<MjExposedSet>> exposedSetsMap = new HashMap<>();

	/** 赖子牌ID, 0表示无赖子 */
	private int laiZiTileId;

	/** 当前赖子牌的显示值(翻牌确定) */
	private int laiZiFlipTile;

	/** 每个座位是否有副露(开口笑规则用) */
	private final Map<Integer, Boolean> openedMap = new HashMap<>();

	/** 弃牌堆(每座的出牌历史) */
	private final Map<Integer, List<Integer>> discardPileMap = new HashMap<>();

	/** claim相关: 等待claim的座位列表 */
	private final List<Integer> pendingClaimSeats = new ArrayList<>();

	/** claim相关: 当前正在等待claim的牌ID */
	private int claimTileId;

	/** claim相关: 出牌者的座位 */
	private int claimFromSeat;

	/** 杠上开花标记(杠后补牌胡) */
	private boolean gangShangKaiHua;

	/** 抢杠胡标记 */
	private boolean qiangGangHu;

	/** 海底标记(牌墙最后一张) */
	private boolean haiDi;

	/** AI 视野等级：0=正常, 1=半透视(知剩余牌池), 2=全透视(知他人手牌) */
	private int visionLevel;
	/** AI 智能等级：0=最笨(摸什么打什么/自动过), 1=基础策略, 2=高级策略 */
	private int aiLevel = 2;

	// --- Getters & Setters ---

	public int getDealerSeat() {
		return dealerSeat;
	}

	public void setDealerSeat(int dealerSeat) {
		this.dealerSeat = dealerSeat;
	}

	public int getDrawnTile() {
		return drawnTile;
	}

	public void setDrawnTile(int drawnTile) {
		this.drawnTile = drawnTile;
	}

	public boolean isTileDrawn() {
		return tileDrawn;
	}

	public void setTileDrawn(boolean tileDrawn) {
		this.tileDrawn = tileDrawn;
	}

	public boolean isDiscardPromptSent() {
		return discardPromptSent;
	}

	public void setDiscardPromptSent(boolean discardPromptSent) {
		this.discardPromptSent = discardPromptSent;
	}

	public int getLastDiscardTile() {
		return lastDiscardTile;
	}

	public void setLastDiscardTile(int lastDiscardTile) {
		this.lastDiscardTile = lastDiscardTile;
	}

	public int getLastDiscardSeat() {
		return lastDiscardSeat;
	}

	public void setLastDiscardSeat(int lastDiscardSeat) {
		this.lastDiscardSeat = lastDiscardSeat;
	}

	public void resetTurn() {
		tileDrawn = false;
		discardPromptSent = false;
		drawnTile = 0;
	}

	public void resetRound() {
		dealerSeat = 0;
		lastDiscardTile = 0;
		lastDiscardSeat = -1;
		exposedSetsMap.clear();
		openedMap.clear();
		discardPileMap.clear();
		laiZiTileId = 0;
		laiZiFlipTile = 0;
		gangShangKaiHua = false;
		qiangGangHu = false;
		haiDi = false;
		resetTurn();
	}

	// --- 副露区管理 ---

	/** 获取某个座位的副露区（只读，不存在返回空list，不创建新对象） */
	public List<MjExposedSet> getExposedSets(int seat) {
		return exposedSetsMap.getOrDefault(seat, Collections.emptyList());
	}

	/** 获取或创建某个座位的副露区（写操作用） */
	private List<MjExposedSet> getOrCreateExposedSets(int seat) {
		return exposedSetsMap.computeIfAbsent(seat, k -> new ArrayList<>());
	}

	/** 添加副露 */
	public void addExposedSet(int seat, MjExposedSet set) {
		getOrCreateExposedSets(seat).add(set);
		openedMap.put(seat, true);
	}

	/**
	 * 某座位是否有副露
	 */
	public boolean hasOpened(int seat) {
		return openedMap.getOrDefault(seat, false);
	}

	// --- 弃牌堆管理 ---

	/**
	 * 记录出牌
	 */
	public void addDiscard(int seat, int tileId) {
		discardPileMap.computeIfAbsent(seat, k -> new ArrayList<>()).add(tileId);
	}

	/**
	 * 获取弃牌堆
	 */
	public List<Integer> getDiscardPile(int seat) {
		return discardPileMap.getOrDefault(seat, Collections.emptyList());
	}

	// --- 赖子 ---

	public int getLaiZiTileId() {
		return laiZiTileId;
	}

	public void setLaiZiTileId(int laiZiTileId) {
		this.laiZiTileId = laiZiTileId;
	}

	public int getLaiZiFlipTile() {
		return laiZiFlipTile;
	}

	public void setLaiZiFlipTile(int laiZiFlipTile) {
		this.laiZiFlipTile = laiZiFlipTile;
	}

	// --- Claim管理 ---

	/**
	 * 设置claim信息
	 */
	public void setClaimInfo(int tileId, int fromSeat, List<Integer> waitingSeats) {
		this.claimTileId = tileId;
		this.claimFromSeat = fromSeat;
		this.pendingClaimSeats.clear();
		this.pendingClaimSeats.addAll(waitingSeats);
	}

	/**
	 * 某座位完成claim(响应或pass)
	 */
	public void removeClaimSeat(int seat) {
		pendingClaimSeats.remove(Integer.valueOf(seat));
	}

	/**
	 * 是否还有待响应的claim
	 */
	public boolean hasPendingClaims() {
		return !pendingClaimSeats.isEmpty();
	}

	public List<Integer> getPendingClaimSeats() {
		return pendingClaimSeats;
	}

	public int getClaimTileId() {
		return claimTileId;
	}

	public int getClaimFromSeat() {
		return claimFromSeat;
	}

	// --- 特殊胡牌标记 ---

	public boolean isGangShangKaiHua() {
		return gangShangKaiHua;
	}

	public void setGangShangKaiHua(boolean gangShangKaiHua) {
		this.gangShangKaiHua = gangShangKaiHua;
	}

	public boolean isQiangGangHu() {
		return qiangGangHu;
	}

	public void setQiangGangHu(boolean qiangGangHu) {
		this.qiangGangHu = qiangGangHu;
	}

	public boolean isHaiDi() {
		return haiDi;
	}

	public void setHaiDi(boolean haiDi) {
		this.haiDi = haiDi;
	}

	public int getVisionLevel() {
		return visionLevel;
	}

	public void setVisionLevel(int visionLevel) {
		this.visionLevel = visionLevel;
	}

	public int getAiLevel() {
		return aiLevel;
	}

	public void setAiLevel(int aiLevel) {
		this.aiLevel = aiLevel;
	}
}
