package game.manager.model;

import game.Game;

public class Table {

	private String tableId;

	public String getTableId() {
		return tableId;
	}

	public void setTableId(String tableId) {
		this.tableId = tableId;
	}

	public void start() {
		Game.getInstance().registerSerialTimer(getGroupIndex(), 1000, 1000, -1, this::tableLoop, this);
	}

	/**
	 * 获取 线程组处理id
	 */
	public int getGroupIndex() {
		return Integer.parseInt(getTableId().substring(getTableId().length() - 1));
	}

	/**
	 * 桌子循环
	 */
	public boolean tableLoop(Table table) {
		return false;
	}
}
