package game.manager.table.mj;

import game.manager.table.Table;
import game.manager.table.TableUser;
import game.manager.table.card.mj.MjTilePool;
import game.manager.table.cards.Card;
import msg.registor.enums.TableState;
import msg.registor.message.GMsg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.GameProto;

import java.util.List;

/**
 * 麻将出牌服务
 * 处理摸牌、出牌、胡牌判定等核心逻辑
 */
public class MjPlayService {

    private static final Logger logger = LoggerFactory.getLogger(MjPlayService.class);

    /**
     * 当前玩家摸牌
     *
     * @return 摸到的牌ID，-1表示牌墙已空
     */
    public static int drawTile(Table table) {
        MjTilePool tilePool = table.getMjTilePool();
        if (tilePool == null || tilePool.remaining() <= 0) {
            logger.warn("牌墙已空, table: {}", table.getTableId());
            return -1;
        }

        int seat = table.getOp().getCurrOpSeat();
        TableUser user = table.getSeatUser(seat);
        if (user == null) {
            logger.error("摸牌时玩家不存在, table: {}, seat: {}", table.getTableId(), seat);
            return -1;
        }

        int tileId = tilePool.drawTile();
        user.addCards(new Card(tileId));
        table.getMjContext().setDrawnTile(tileId);
        table.getMjContext().setTileDrawn(true);

        // 通知手牌
        tilePool.sendHandNotice(table.getSeatUsers());

        logger.info("麻将摸牌, table: {}, seat: {}, tile: {}, 剩余: {}",
                table.getTableId(), seat, tileId, tilePool.remaining());
        return tileId;
    }

    /**
     * 处理玩家出牌请求
     */
    public static boolean applyDiscard(Table table, int userId, GameProto.OpInfo opInfo) {
        int seat = table.getOp().getCurrOpSeat();
        TableUser user = table.getSeatUser(seat);
        if (user == null || user.getUserId() != userId) {
            logger.warn("出牌操作座位不匹配, table: {}, seat: {}, userId: {}",
                    table.getTableId(), seat, userId);
            return false;
        }

        if (opInfo.getOpCardsCount() == 0) {
            logger.warn("出牌未指定牌, table: {}, userId: {}", table.getTableId(), userId);
            return false;
        }

        int tileId = opInfo.getOpCards(0).getCards(0).getValue();

        // 从手牌中移除
        boolean removed = user.removeCardsByProtoIds(java.util.Collections.singletonList(tileId));
        if (!removed) {
            logger.warn("出牌不在手牌中, table: {}, userId: {}, tile: {}",
                    table.getTableId(), userId, tileId);
            return false;
        }

        // 更新上下文
        MjTableContext ctx = table.getMjContext();
        ctx.setLastDiscardTile(tileId);
        ctx.setLastDiscardSeat(seat);
        ctx.resetTurn();

        // 广播出牌通知
        GameProto.NotMjState not = GameProto.NotMjState.newBuilder()
                .setOpSeat(seat)
                .setTileId(tileId)
                .setAction(GameProto.MjAction.MJ_DISCARD_TILE)
                .setWallLeft(table.getMjTilePool().remaining())
                .build();
        table.sendTableMessage(not, GMsg.MJ_TILE_NOT);

        logger.info("麻将出牌, table: {}, seat: {}, tile: {}", table.getTableId(), seat, tileId);
        return true;
    }

    /**
     * 超时自动出牌(出刚摸到的牌)
     */
    public static void autoDiscard(Table table) {
        int seat = table.getOp().getCurrOpSeat();
        TableUser user = table.getSeatUser(seat);
        if (user == null) {
            return;
        }

        MjTableContext ctx = table.getMjContext();
        int tileId = ctx.getDrawnTile();
        if (tileId == 0) {
            // 没有记录摸的牌，出最后一张
            List<Card> cards = user.getCards();
            if (!cards.isEmpty()) {
                tileId = cards.get(cards.size() - 1).getId();
            } else {
                return;
            }
        }

        boolean removed = user.removeCardsByProtoIds(java.util.Collections.singletonList(tileId));
        if (!removed) {
            logger.error("自动出牌失败, table: {}, seat: {}, tile: {}", table.getTableId(), seat, tileId);
            return;
        }

        ctx.setLastDiscardTile(tileId);
        ctx.setLastDiscardSeat(seat);
        ctx.resetTurn();

        // 广播
        GameProto.NotMjState not = GameProto.NotMjState.newBuilder()
                .setOpSeat(seat)
                .setTileId(tileId)
                .setAction(GameProto.MjAction.MJ_DISCARD_TILE)
                .setWallLeft(table.getMjTilePool().remaining())
                .build();
        table.sendTableMessage(not, GMsg.MJ_TILE_NOT);

        logger.info("麻将超时自动出牌, table: {}, seat: {}, tile: {}", table.getTableId(), seat, tileId);
    }

    /**
     * 移动到下一个玩家
     */
    public static void nextPlayer(Table table) {
        int currSeat = table.getOp().getCurrOpSeat();
        int nextSeat = table.nextSeat(currSeat);
        table.getOp().setCurrOpSeat(nextSeat);
        table.getMjContext().resetTurn();
    }

    /**
     * 检查是否有人能胡/碰/杠(吃碰杠胡 TODO 后续迭代)
     * 当前版本：直接返回无人响应，进入下一个玩家摸牌
     */
    public static boolean checkClaim(Table table) {
        // TODO 吃碰杠胡判定 - 后续迭代
        // 当前实现：无人响应，直接下一个玩家
        return false;
    }

    /**
     * 结束本局(流局)
     */
    public static void finishGame(Table table, String reason) {
        logger.info("麻将局结束, table: {}, reason: {}", table.getTableId(), reason);

        // 通知结果
        GameProto.NotResult result = GameProto.NotResult.newBuilder()
                .setWinner(-1)
                .build();
        table.sendTableMessage(result, GMsg.NOT_RESULT);

        table.upNextState(TableState.TABLE_OVER);
    }
}
