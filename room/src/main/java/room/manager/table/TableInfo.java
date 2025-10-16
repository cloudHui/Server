package room.manager.table;

import java.util.HashSet;
import java.util.Set;

import model.TableModel;
import msg.registor.enums.TableState;
import room.manager.user.User;

/**
 * @author admin
 * @className TableInfo
 * @description
 * @createDate 2025/10/16 11:39
 */
public class TableInfo {

	private final String tableId;

	private final int creatorId;

	private final TableModel model;

	private int ownerId;

	private TableState stat = TableState.WAITE;

	private final Set<User> tableRoles = new HashSet<>();

	public TableInfo(String tableId, int creatorId, TableModel model) {
		this.tableId = tableId;
		this.creatorId = creatorId;
		this.model = model;
	}

	public String getTableId() {
		return tableId;
	}

	public int getCreatorId() {
		return creatorId;
	}

	public TableModel getModel() {
		return model;
	}

	public int getOwnerId() {
		return ownerId;
	}

	public TableState getStat() {
		return stat;
	}

	public Set<User> getTableRoles() {
		return tableRoles;
	}

	public void setOwnerId(int ownerId) {
		this.ownerId = ownerId;
	}

	public void setStat(TableState stat) {
		this.stat = stat;
	}

	public void joinRole(User user) {
		tableRoles.add(user);
		//Todo 机器人房间只要加入了真人玩家就直接开始
	}

	public void removeUser(User user) {
		tableRoles.remove(user);
	}

	public boolean canJoin() {
		return stat == TableState.WAITE && tableRoles.size() < model.getNum();
	}
}
