package game.manager;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import game.Game;
import game.manager.model.Table;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.other.TimeUtil;

public class TableManager {

	private final static Logger logger = LoggerFactory.getLogger(TableManager.class);

	//初始桌子序号
	private final static int BASE_INDEX = 100000;
	private final Map<String, Table> tableMap;
	/**
	 * 当前初始化桌子号
	 */
	private int currIndex;
	//桌子号的头
	private String idHead;

	public TableManager() {
		tableMap = new ConcurrentHashMap<>();
		resetTableId();
		registerResetTableTask();
	}

	public String getIdHead() {
		return idHead;
	}

	public void setIdHead(String idHead) {
		this.idHead = idHead;
	}

	public void addTable(Table table) {
		tableMap.put(table.getTableId(), table);
	}

	public Table getTable(String tableId) {
		return tableMap.get(tableId);
	}

	public Table delTable(String tableId) {
		return tableMap.remove(tableId);
	}

	/**
	 * 每天 0 点 重置 桌子号和其他参数
	 */
	private void registerResetTableTask() {
		long nextZero = TimeUtil.curZeroHourTime(System.currentTimeMillis()) + TimeUtil.DAY;
		Game.getInstance().registerTimer(nextZero, TimeUtil.DAY, -1, game -> {
			resetTableId();
			return false;
		}, this);
	}

	/**
	 * 获取桌子号
	 */
	public String getTableId() {
		synchronized (this) {
			String tableId = getIdHead() + currIndex++;
			logger.info("[create new table id:{}]", tableId);
			return tableId;
		}
	}

	/**
	 * 重置id 头尾拼接字段
	 */
	public void resetTableId() {
		synchronized (this) {
			SimpleDateFormat sp = new SimpleDateFormat("yyMMddHH");
			setIdHead(sp.format(new Date()));
			currIndex = BASE_INDEX;
			logger.info("[resettableId {} head:{} baseId:{}]", new Timestamp(System.currentTimeMillis()), idHead, currIndex);
		}
	}
}
