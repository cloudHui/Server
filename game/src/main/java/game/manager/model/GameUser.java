package game.manager.model;

import java.util.HashSet;
import java.util.Set;

public class GameUser {
	private final Set<String> tableIds = new HashSet<>();
	private int userId;
	private boolean onLine;
	private boolean sit;
	private long diamond;

	public int getUserId() {
		return userId;
	}

	public void setUserId(int userId) {
		this.userId = userId;
	}

	public boolean getOnLine() {
		return onLine;
	}

	public void setOnLine(boolean onLine) {
		this.onLine = onLine;
	}

	public boolean getSit() {
		return sit;
	}

	public void setSit(boolean sit) {
		this.sit = sit;
	}

	public long getDiamond() {
		return diamond;
	}

	public void setDiamond(long diamond) {
		this.diamond = diamond;
	}

	public Set<String> getTableIds() {
		return tableIds;
	}

	public void addTable(String tableId) {
		this.tableIds.add(tableId);
	}

	public void removeTable(String tableId) {
		this.tableIds.remove(tableId);
	}

	@Override
	public String toString() {
		return "User{" +
				"userId=" + userId +
				", onLine=" + onLine +
				", sit=" + sit +
				", diamond=" + diamond +
				'}';
	}
}
