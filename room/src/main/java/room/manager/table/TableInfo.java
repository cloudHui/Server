package room.manager.table;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import model.tablemodel.TableModel;
import proto.ConstProto;
import proto.ModelProto;
import room.manager.user.User;

/**
 * @author admin
 * @className TableInfo
 * @description
 * @createDate 2025/10/16 11:39
 */
public class TableInfo {

	private final long tableId;

	private final int creatorId;

	private final TableModel model;

	private int ownerId;

	/**
	 * 桌子状态
	 */
	private ConstProto.TableState tableState = ConstProto.TableState.WAIT;

	private final Set<User> tableRoles = Collections.newSetFromMap(new ConcurrentHashMap<>());

	public TableInfo(long tableId, int creatorId, TableModel model) {
		this.tableId = tableId;
		this.creatorId = creatorId;
		this.model = model;
	}

	public Long getTableId() {
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

	public ConstProto.TableState getTableState() {
		return tableState;
	}

	public Set<User> getTableRoles() {
		return tableRoles;
	}

	public void setOwnerId(int ownerId) {
		this.ownerId = ownerId;
	}

	public void setTableState(ConstProto.TableState tableState) {
		this.tableState = tableState;
	}

	public void joinRole(User user) {
		tableRoles.add(user);
		user.addTable(tableId);
	}

	public void removeUser(User user) {
		tableRoles.remove(user);
		user.removeTable(tableId);
	}

	public boolean canJoin() {
		return tableState == ConstProto.TableState.WAIT && tableRoles.size() < model.getSeatNum();
	}

	public ModelProto.RoomTableInfo getTableInfo() {
		ModelProto.RoomTableInfo.Builder builder = ModelProto.RoomTableInfo.newBuilder()
				.setRoomId(model.getId())
				.setTableId(tableId)
				.setOwnerId(ownerId)
				.setStat(tableState.getNumber())
				.setCreatorId(creatorId)
				.setGameType(model.getType());

		for (User user : tableRoles) {
			if (user != null) {
				builder.addTableRoles(user.getRole());
			}
		}

		return builder.build();
	}
}
