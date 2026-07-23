package game.manager.table;

import game.manager.table.banner.Banner;
import game.manager.table.card.poll.CardPool;
import game.manager.table.cards.Card;
import game.manager.table.ddz.DdzBidService;
import game.manager.table.ddz.DdzHand;
import game.manager.table.ddz.DdzPlayService;
import game.manager.table.ddz.DdzTableContext;
import game.manager.table.replay.DdzReplayRecorder;
import game.manager.table.replay.ReplayRecorder;
import model.tablemodel.TableModel;
import msg.registor.enums.TableState;
import msg.registor.message.GMsg;
import net.client.Sender;
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
    public int getGameType() {
        return 2;
    }

    @Override
    public void dealCards() {
        cardPool.dealInitCard();
    }

    @Override
    public void resetGameContext() {
        banner.reset();
        ddz.resetHand();
    }

    @Override
    public GameResult createGameResult() {
        DdzGameResult result = new DdzGameResult();
        result.setTotalRounds(getTableModel().getTotalRounds());
        return result;
    }

    @Override
    public ReplayRecorder createReplayRecorder() {
        return new DdzReplayRecorder(getTableId(), getCurrentRound());
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
        int seat = user.getSeated();
        if (seat < 0) return;

        // 1. 同步手牌(自己的牌有值, 别人的牌值为0)
        cardPool.sendInitCardNotice(getSeatUsers());

        // 2. 同步桌子状态
        GameProto.NotTableState stateNot = GameProto.NotTableState.newBuilder()
                .setState(getTableState().getId())
                .setStateStart(getStateStartTime())
                .setStateDuration(getTableState().getOverTime())
                .build();
        user.sendRoleMessage(stateNot, GMsg.NOT_STATE, getTableId());

        // 3. 如果当前有出牌阶段的操作, 重新通知当前操作
        TableState ts = getTableState();
        if (ts == TableState.IDLE_CARD || ts == TableState.CARD) {
            DdzHand lastHand = ddz.getLastHand();
            int opSeat = getOp().getCurrOpSeat();
            if (opSeat >= 0) {
                GameProto.OpInfo.Builder choiceBuilder = GameProto.OpInfo.newBuilder()
                        .setChoice(ConstProto.Operation.PLAY);
                if (lastHand != null) {
                    GameProto.CardInfo.Builder cardInfoBuilder = GameProto.CardInfo.newBuilder()
                            .setType(lastHand.getType());
                    for (Card c : lastHand.getCards()) {
                        cardInfoBuilder.addCards(GameProto.Card.newBuilder().setValue(c.getId()));
                    }
                    choiceBuilder.addOpCards(cardInfoBuilder.build());
                    // 有上一手牌时, 也给PASS选项
                    GameProto.NotOperation notOp = GameProto.NotOperation.newBuilder()
                            .setWait(TableState.IDLE_CARD.getOverTime())
                            .setOpSeat(opSeat)
                            .addChoice(choiceBuilder.build())
                            .addChoice(GameProto.OpInfo.newBuilder().setChoice(ConstProto.Operation.PASS).build())
                            .build();
                    user.sendRoleMessage(notOp, GMsg.NOT_OP, getTableId());
                } else {
                    GameProto.NotOperation notOp = GameProto.NotOperation.newBuilder()
                            .setWait(TableState.IDLE_CARD.getOverTime())
                            .setOpSeat(opSeat)
                            .addChoice(choiceBuilder.build())
                            .build();
                    user.sendRoleMessage(notOp, GMsg.NOT_OP, getTableId());
                }
            }
        } else if (ts == TableState.IDLE_ROB || ts == TableState.ROB) {
            int opSeat = getOp().getCurrOpSeat();
            if (opSeat >= 0) {
                GameProto.NotOperation notOp = GameProto.NotOperation.newBuilder()
                        .setWait(TableState.IDLE_ROB.getOverTime())
                        .setOpSeat(opSeat)
                        .addChoice(GameProto.OpInfo.newBuilder().setChoice(ConstProto.Operation.CALL).build())
                        .build();
                user.sendRoleMessage(notOp, GMsg.NOT_OP, getTableId());
            }
        }
    }

    // ======================== DDZ特有getter ========================

    public CardPool getCardPool() {
        return cardPool;
    }

    public Banner getBanner() {
        return banner;
    }

    public DdzTableContext getDdz() {
        return ddz;
    }
}
