package game.manager.table.mj;

import model.tablemodel.TableModel;

/**
 * 麻将计番接口
 * 通用层定义，特殊玩法各自实现
 */
public interface MjScoring {

	/**
	 * 计算一次胡牌的番数
	 *
	 * @param winResult 胡牌结果
	 * @param ctx       麻将上下文
	 * @param model     桌子配置
	 * @return 番数(>=1)
	 */
	int calcFan(MjWinResult winResult, MjTableContext ctx, TableModel model);

	/**
	 * 计算单个玩家的结算分数
	 *
	 * @param winResult 胡牌结果
	 * @param fan       番数
	 * @param ctx       麻将上下文
	 * @param model     桌子配置
	 * @param seatNum   座位数
	 * @return 每个座位的得分/失分数组(正=赢, 负=输)
	 */
	int[] settle(MjWinResult winResult, int fan, MjTableContext ctx, TableModel model, int seatNum);
}
