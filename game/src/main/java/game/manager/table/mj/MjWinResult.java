package game.manager.table.mj;

import game.manager.table.cards.Card;

import java.util.ArrayList;
import java.util.List;

/**
 * 麻将胡牌结果
 * 通用层产出，特殊层(Scoring)根据此结果算番
 */
public class MjWinResult {

	/** 胡牌玩家ID */
	private int winnerId;

	/** 胡的那张牌ID */
	private int winTile;

	/** 是否自摸 */
	private boolean ziMo;

	/** 是否杠上开花 */
	private boolean gangShangKaiHua;

	/** 是否抢杠胡 */
	private boolean qiangGangHu;

	/** 是否海底捞月(牌墙最后一张自摸) */
	private boolean haiDi;

	/** 是否点炮胡 */
	private boolean dianPao;

	/** 点炮者座位(点炮胡时有效) */
	private int dianPaoSeat;

	/** 胡牌时的手牌(不含已副露的牌) */
	private List<Card> handTiles;

	/** 副露区(碰/杠/吃亮出的牌组) */
	private List<MjExposedSet> exposedSets;

	public MjWinResult() {
		this.handTiles = new ArrayList<>();
		this.exposedSets = new ArrayList<>();
	}

	// --- 基本信息 ---

	public int getWinnerId() { return winnerId; }
	public void setWinnerId(int winnerId) { this.winnerId = winnerId; }
	public int getWinTile() { return winTile; }
	public void setWinTile(int winTile) { this.winTile = winTile; }

	// --- 胡牌方式标志 ---

	public boolean isZiMo() { return ziMo; }
	public void setZiMo(boolean ziMo) { this.ziMo = ziMo; }
	public boolean isGangShangKaiHua() { return gangShangKaiHua; }
	public void setGangShangKaiHua(boolean gangShangKaiHua) { this.gangShangKaiHua = gangShangKaiHua; }
	public boolean isQiangGangHu() { return qiangGangHu; }
	public void setQiangGangHu(boolean qiangGangHu) { this.qiangGangHu = qiangGangHu; }
	public boolean isHaiDi() { return haiDi; }
	public void setHaiDi(boolean haiDi) { this.haiDi = haiDi; }
	public boolean isDianPao() { return dianPao; }
	public void setDianPao(boolean dianPao) { this.dianPao = dianPao; }
	public int getDianPaoSeat() { return dianPaoSeat; }
	public void setDianPaoSeat(int dianPaoSeat) { this.dianPaoSeat = dianPaoSeat; }

	// --- 手牌与副露 ---

	public List<Card> getHandTiles() { return handTiles; }
	public void setHandTiles(List<Card> handTiles) { this.handTiles = handTiles; }
	public List<MjExposedSet> getExposedSets() { return exposedSets; }
	public void setExposedSets(List<MjExposedSet> exposedSets) { this.exposedSets = exposedSets; }
}
