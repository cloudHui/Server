package game.manager.table;

import game.manager.table.banner.Banner;
import game.manager.table.card.poll.CardPool;
import game.manager.table.ddz.*;
import game.manager.table.replay.ReplayRecorder;
import model.tablemodel.TableModel;
import msg.registor.enums.TableState;
import net.client.Sender;
import net.message.TCPMessage;
import proto.ConstProto;
import proto.GameProto;
import proto.ModelProto;

/**
 * 斗地主桌子
 * 包含斗地主特有的牌池、叫分、出牌上下文等
 */
public class DdzTable extends Table {

	private final CardPool cardPool;
	private final Banner banner;
	private final DdzTableContext ddz = new DdzTableContext();

	public DdzTable(long tableId, TableModel model, ModelProto.RoomRole creator) {
		super(tableId, model, creator);
		this.cardPool = new CardPool(this);
		this.banner = new Banner();
	}

	// ======================== 抽象方法实现 ========================

	@Override
	public int getGameType() { return 2; }

	@Override
	public void dealCards() {
		cardPool.dealInitCard();
	}

	@Override
	public void resetGameContext() {
		banner.reset();
		ddz.reset();
	}

	@Override
	public GameResult createGameResult() {
		DdzGameResult result = new DdzGameResult();
		result.setTotalRounds(getTableModel().getTotalRounds());
		return result;
	}

	@Override
	public ReplayRecorder createReplayRecorder() {
		return null; // DDZ回放暂不实现
	}

	@Override
	public void initGameConfig() {
		// DDZ无特殊初始化
	}

	@Override
	public int processOp(int userId, GameProto.OpInfo op, Sender sender, long mapId, int sequence) {
		TableState ts = getTableState();
		if (ts == TableState.IDLE_ROB) {
			return DdzBidService.apply(this, userId, op);
		}
		if (ts == TableState.IDLE_CARD) {
			return DdzPlayService.apply(this, userId, op);
		}
		return ConstProto.Result.OP_CURR_ERROR_VALUE;
	}

	@Override
	public void syncGameState(TableUser user) {
		// DDZ断线重连: 暂不实现, 后续扩展
	}

	// ======================== DDZ特有getter ========================

	public CardPool getCardPool() { return cardPool; }
	public Banner getBanner() { return banner; }
	public DdzTableContext getDdz() { return ddz; }
}
