package game.manager.table;

import java.util.HashMap;
import java.util.Map;

import game.Game;
import game.manager.table.state.TableStateHandleManager;
import model.TableModel;
import msg.registor.enums.TableState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.ConstProto;
import proto.GameProto;
import proto.ServerProto;

/**
 * 游戏桌子模型
 * 代表一个游戏桌子的状态和行为
 */
public class Table {
	private static final Logger logger = LoggerFactory.getLogger(Table.class);

	private final String tableId;

	private final TableModel tableModel;

	private final ServerProto.RoomRole creator;

	/**
	 * 桌子上的玩家
	 */
	private final Map<Integer, TableUser> users = new HashMap<>();

	/**
	 * 桌子上的玩家
	 */
	private final Map<Integer, Integer> seatUsers = new HashMap<>();

	/**
	 * 牌桌运行状态
	 */
	private TableState tableState = TableState.WAITING;

	/**
	 * 状态开始时间
	 */
	private long stateStartTime;

	private int errorTimes;

	/**
	 * 最大错误次数
	 */
	private static final int MAX_ERROR = 100;

	public Table(String tableId, TableModel model, ServerProto.RoomRole creator) {
		this.tableId = tableId;
		this.creator = creator;
		this.tableModel = model;
		logger.debug("创建桌子实例, tableId: {}", tableId);
	}

	public String getTableId() {
		return tableId;
	}

	public ServerProto.RoomRole getCreator() {
		return creator;
	}

	public int getRoomId() {
		return tableModel.getId();
	}

	public Map<Integer, TableUser> getUsers() {
		return users;
	}

	public TableState getTableState() {
		return tableState;
	}

	public void setTableState(TableState tableState) {
		this.tableState = tableState;
	}

	public long getStateStartTime() {
		return stateStartTime;
	}

	public void setStateStartTime(long stateStartTime) {
		this.stateStartTime = stateStartTime;
	}

	public void addErrorTime() {
		if (++errorTimes >= MAX_ERROR) {
			setTableState(TableState.TABLE_OVER);
		}
	}

	public TableUser getUser(int userId, int gateId, GameProto.ReqEnterTable req) {
		TableUser tableUser = users.get(userId);
		if (tableUser == null) {
			tableUser = new TableUser(userId, req.getHead().toStringUtf8(), req.getNick().toStringUtf8(), gateId);
			users.put(userId, tableUser);
		}
		return tableUser;
	}

	/**
	 * 桌子是否坐满了
	 *
	 * @return 是否坐满
	 */
	public boolean sitFull() {
		return users.size() >= tableModel.getNum();
	}

	/**
	 * 桌子空了
	 */
	public boolean isEmpty() {
		return users.isEmpty();
	}

	/**
	 * 开始游戏了
	 *
	 * @return 是否开始游戏
	 */
	public boolean gaming() {
		return tableState != TableState.WAITING && tableState != TableState.TABLE_OVER;
	}

	/**
	 * 启动桌子逻辑循环
	 */
	public void start() {
		try {
			int groupIndex = getGroupIndex();
			Game.getInstance().registerSerialTimer(groupIndex, 1000, 500, -1, this::tableLoop, this);
			logger.info("启动桌子逻辑循环, tableId: {}, groupIndex: {}", tableId, groupIndex);
		} catch (Exception e) {
			logger.error("启动桌子逻辑循环失败, tableId: {}", tableId, e);
		}
	}

	/**
	 * 获取线程组处理ID
	 * 根据桌子ID的最后一位数字确定线程组,实现负载均衡
	 */
	public int getGroupIndex() {
		try {
			if (tableId == null || tableId.length() == 0) {
				logger.warn("桌子ID为空,使用默认线程组0");
				return 0;
			}

			char lastChar = tableId.charAt(tableId.length() - 1);
			int groupIndex = Character.getNumericValue(lastChar) % 10;
			logger.debug("计算线程组索引, tableId: {}, groupIndex: {}", tableId, groupIndex);
			return groupIndex;
		} catch (Exception e) {
			logger.error("计算线程组索引失败, tableId: {}", tableId, e);
			return 0;
		}
	}

	/**
	 * 桌子主循环
	 * 处理桌子的游戏逻辑更新
	 *
	 * @param table 桌子实例
	 * @return 是否需要结束任务循环 true-结束 false-继续
	 */
	public boolean tableLoop(Table table) {
		try {
			return TableStateHandleManager.handle(this);
		} catch (Exception e) {
			logger.error("桌子循环执行异常, tableId: {}", tableId, e);
			return false;
		}
	}

	/**
	 * 给玩家发牌
	 */
	public void sendCard() {
	}

	/**
	 * 添加玩家到桌子
	 *
	 * @param user 玩家对象
	 * @return 是否入桌成功
	 */
	public int addUser(TableUser user) {
		try {
			if (user == null) {
				logger.warn("尝试添加空玩家到桌子, tableId: {}", tableId);
				return ConstProto.Result.ROLE_NULL_VALUE;
			}

			if (sitFull()) {
				logger.warn("桌子满了, tableId: {}", tableId);
				return ConstProto.Result.TABLE_FULL_VALUE;
			}
			if (isEmpty()) {
				start();
			}
			int seat = occupySeat(user);
			logger.info("玩家加入桌子, userId: {}, tableId: {} seat:{}", user.getUserId(), tableId, seat);
			return seat == -1 ? ConstProto.Result.TABLE_FULL_VALUE : ConstProto.Result.SUCCESS_VALUE;
		} catch (Exception e) {
			logger.error("添加玩家到桌子失败, userId: {}, tableId: {}", user != null ? user.getUserId() : "null", tableId, e);
			return ConstProto.Result.ROLE_ERROR_VALUE;
		}
	}

	/**
	 * 占座
	 *
	 * @param user 玩家
	 * @return 坐下的位置
	 */
	private int occupySeat(TableUser user) {
		for (int index = 0; index < tableModel.getNum(); index++) {
			if (!seatUsers.containsKey(index)) {
				seatUsers.put(index, user.getUserId());
				user.setSeated(index);
				return index;
			}
		}
		return -1;
	}

	/**
	 * 从桌子移除玩家
	 */
	public boolean removeUser(TableUser user) {
		try {
			if (user == null) {
				logger.warn("尝试从桌子移除空玩家, tableId: {}", tableId);
				return false;
			}

			// TODO: 实现具体的移除逻辑
			user.removeTable(tableId);

			logger.info("玩家离开桌子, userId: {}, tableId: {}", user.getUserId(), tableId);
			return true;
		} catch (Exception e) {
			logger.error("从桌子移除玩家失败, userId: {}, tableId: {}",
					user != null ? user.getUserId() : "null", tableId, e);
			return false;
		}
	}


	@Override
	public String toString() {
		return String.format("Table{tableId='%s'}", tableId);
	}
}