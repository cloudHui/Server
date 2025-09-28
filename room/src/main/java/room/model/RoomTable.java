package room.model;

import java.util.HashSet;
import java.util.Set;

/**
 * @author admin
 * @className RoomTable
 * @description
 * @createDate 2025/9/28 15:15
 */
public class RoomTable {

	private final String tableId;

	private final Set<RoomRole> roles = new HashSet<>();

	public RoomTable(String tableId) {
		this.tableId = tableId;
	}

	public String getTableId() {
		return tableId;
	}

	public void addRole(RoomRole role) {
		roles.add(role);
	}

	public Set<RoomRole> getRoles() {
		return roles;
	}
}
