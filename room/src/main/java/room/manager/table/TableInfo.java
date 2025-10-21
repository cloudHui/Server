package room.manager.table;

import java.util.HashSet;
import java.util.Set;

import com.google.protobuf.ByteString;
import model.TableModel;
import proto.ConstProto;
import proto.ServerProto;
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
	private ConstProto.TableState tableState = ConstProto.TableState.WAITE;

	private final Set<User> tableRoles = new HashSet<>();

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
	}

	public boolean canJoin() {
		return tableState == ConstProto.TableState.WAITE && tableRoles.size() < model.getSeatNum();
	}

	public ServerProto.RoomTableInfo getTableInfo() {
		ServerProto.RoomTableInfo.Builder builder = ServerProto.RoomTableInfo.newBuilder()
				.setRoomId(model.getId())
				.setTableId(tableId)
				.setOwnerId(ownerId)
				.setStat(tableState.getNumber())
				.setCreatorId(ownerId);

		for (User user : tableRoles) {
			if (user != null) {
				builder.addTableRoles(user.getRole());
			}
		}

		return builder.build();
	}
}
