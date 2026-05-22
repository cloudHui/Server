package game.manager.table.mj;

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
		resetTurn();
	}
}
