package room.manager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import proto.ServerProto;

public class User {

	private final int userId;

	private ServerProto.RoomRole role;

	private final Map<String, ServerProto.RoomTableInfo> tables = new HashMap<>();

	public User(int userId) {
		this.userId = userId;
	}

	public int getUserId() {
		return userId;
	}

	public void setRole(ServerProto.RoomRole role) {
		this.role = role;
	}

	public ServerProto.RoomRole getRole() {
		return role;
	}

	public void addTables(ServerProto.RoomTableInfo roomTableInfo) {
		tables.put(roomTableInfo.getTableId().toStringUtf8(), roomTableInfo);
	}

	public void removeTables(String tableId) {
		tables.remove(tableId);
	}

	public List<ServerProto.RoomTableInfo> getAllTables() {
		return new ArrayList<>(tables.values());
	}

	public void destroy() {

	}
}