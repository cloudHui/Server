package robot.game;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.ConstProto;
import proto.GameProto;

import java.util.*;

/**
 * Robot游戏会话
 * 维护单局游戏状态：手牌、座位、当前可选操作
 * 供各个Handler共享状态，协调自动出牌流程
 */
public class RobotGameSession {

	private static final Logger logger = LoggerFactory.getLogger(RobotGameSession.class);

	/** 当前手牌ID列表 */
	private final List<Integer> handCards = new ArrayList<>();

	/** 当前座位号 */
	private int mySeat = -1;

	/** 桌子ID */
	private long tableId;

	/** 当前可选操作列表 */
	private final List<GameProto.OpInfo> choices = new ArrayList<>();

	/** 当前操作发送器（由handler注入） */
	private OpSender opSender;

	/** 游戏类型: 1=麻将, 2=斗地主 */
	private int gameType;

	/** 操作序列号 */
	private int seqCounter;

	// ======================== 操作发送接口 ========================

	/** 操作发送接口，由外部注入具体实现 */
	public interface OpSender {
		void sendReqOp(long tableId, GameProto.OpInfo op, int sequence);
	}

	public void setOpSender(OpSender sender) {
		this.opSender = sender;
		logger.info("[Robot seat={} tableId={}] OpSender已注册", mySeat, tableId);
	}

	// ======================== 状态管理 ========================

	/** 初始化手牌（发牌通知时调用） */
	public void initHand(List<GameProto.NCardsInfo> nCardsList, int roleId) {
		handCards.clear();
		for (GameProto.NCardsInfo nCards : nCardsList) {
			if (nCards.getRoleId() == roleId) {
				for (GameProto.Card card : nCards.getCardsList()) {
					if (card.getValue() > 0) {
						handCards.add(card.getValue());
					}
				}
				break;
			}
		}
		logger.info("[Robot seat={} tableId={}] 初始化手牌完成, 数量={}, cards={}",
				mySeat, tableId, handCards.size(), handCards);
	}

	/** 添加一张牌（摸牌时） */
	public void addCard(int cardId) {
		handCards.add(cardId);
		logger.debug("[Robot seat={} tableId={}] 添加牌, tile={}, 手牌数={}",
				mySeat, tableId, cardId, handCards.size());
	}

	/** 移除一张牌（出牌时） */
	public boolean removeCard(int cardId) {
		boolean removed = handCards.remove(Integer.valueOf(cardId));
		if (!removed) {
			logger.warn("[Robot seat={} tableId={}] 移除牌失败, tile={} 不在手牌中, 当前手牌={}",
					mySeat, tableId, cardId, handCards);
		} else {
			logger.debug("[Robot seat={} tableId={}] 移除牌, tile={}, 剩余手牌数={}",
					mySeat, tableId, cardId, handCards.size());
		}
		return removed;
	}

	/** 设置可选操作 */
	public void setChoices(List<GameProto.OpInfo> newChoices) {
		choices.clear();
		choices.addAll(newChoices);
	}

	/** 获取手牌数量 */
	public int getHandSize() { return handCards.size(); }

	/** 获取手牌（只读） */
	public List<Integer> getHandCards() { return Collections.unmodifiableList(handCards); }

	public int getMySeat() { return mySeat; }
	public void setMySeat(int seat) { this.mySeat = seat; }
	public long getTableId() { return tableId; }
	public void setTableId(long tableId) { this.tableId = tableId; }
	public int getGameType() { return gameType; }
	public void setGameType(int gameType) { this.gameType = gameType; }

	// ======================== 自动操作 ========================

	/** 从可选操作中选择一个执行 */
	public void autoChooseAndSend() {
		if (choices.isEmpty()) {
			logger.warn("[Robot seat={} tableId={}] 无可选操作, 跳过", mySeat, tableId);
			return;
		}
		if (opSender == null) {
			logger.error("[Robot seat={} tableId={}] OpSender未注册, 无法发送操作", mySeat, tableId);
			return;
		}

		GameProto.OpInfo chosen = selectBestChoice();
		int seq = ++seqCounter;
		opSender.sendReqOp(tableId, chosen, seq);

		logger.info("[Robot seat={} tableId={}] 自动操作, choice={}, 出牌={}, 手牌数={}, 手牌={}",
				mySeat, tableId, chosen.getChoice(),
				chosen.getOpCardsCount() > 0 ? chosen.getOpCards(0).getCardsList() : "无",
				handCards.size(), handCards);
	}

	/** 选择最佳操作（简单策略） */
	private GameProto.OpInfo selectBestChoice() {
		// 优先级: 胡 > 杠 > 碰 > 吃 > 出牌 > 过
		GameProto.OpInfo best = choices.get(0);
		int bestPriority = getChoicePriority(best.getChoice());

		for (GameProto.OpInfo choice : choices) {
			int priority = getChoicePriority(choice.getChoice());
			if (priority > bestPriority) {
				bestPriority = priority;
				best = choice;
			}
		}

		// 如果选了出牌(PLAY/DISCARD)，需要附带具体的牌
		ConstProto.Operation op = best.getChoice();
		if (op == ConstProto.Operation.PLAY || op == ConstProto.Operation.DISCARD) {
			best = buildPlayOrDiscard(best);
		}

		return best;
	}

	/** 构建出牌操作（附带手牌中选一张） */
	private GameProto.OpInfo buildPlayOrDiscard(GameProto.OpInfo base) {
		if (handCards.isEmpty()) {
			logger.warn("[Robot seat={} tableId={}] 手牌为空, 无法出牌", mySeat, tableId);
			return base;
		}

		// 出最后一张牌（最简单的策略）
		int cardId = handCards.get(handCards.size() - 1);

		GameProto.CardInfo cardInfo = GameProto.CardInfo.newBuilder()
				.addCards(GameProto.Card.newBuilder().setValue(cardId).build())
				.build();

		return GameProto.OpInfo.newBuilder()
				.setChoice(base.getChoice())
				.addOpCards(cardInfo)
				.build();
	}

	/** 操作优先级（越高越优先） */
	private int getChoicePriority(ConstProto.Operation choice) {
		switch (choice) {
			case MJ_HU: return 100;
			case MJ_GANG: return 90;
			case MJ_PENG: return 80;
			case MJ_CHI: return 70;
			case PLAY: return 50;
			case DISCARD: return 50;
			case CALL_SCORE_3: return 40;
			case CALL_SCORE_2: return 30;
			case CALL_SCORE_1: return 20;
			case ROB: return 15;
			case CALL: return 10;
			case PREPARE: return 5;
			case PASS: return 1;
			case MJ_PASS: return 1;
			case NOT_CALL: return 0;
			case NOT_ROB: return 0;
			default: return 0;
		}
	}

	/** 清空状态（新一局） */
	public void reset() {
		logger.info("[Robot seat={} tableId={}] 会话重置", mySeat, tableId);
		handCards.clear();
		choices.clear();
		mySeat = -1;
		seqCounter = 0;
	}
}
