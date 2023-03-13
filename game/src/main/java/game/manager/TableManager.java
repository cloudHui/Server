package game.manager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import game.manager.model.Table;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.utils.RandomUtils;

public class TableManager {

	private final static Logger logger = LoggerFactory.getLogger(TableManager.class);

	/**
	 * 当前初始化桌子号
	 */
	private int randomBeginTableIndex = 0;

	//桌子号的头
	private String idHead;

	//桌子号的尾
	private String idTail;

	private static TableManager instance = new TableManager();

	private Map<String, Table> tableMap;

	public String getIdHead() {
		return idHead;
	}

	public void setIdHead(String idHead) {
		this.idHead = idHead;
	}

	public String getIdTail() {
		return idTail;
	}

	public void setIdTail(String idTail) {
		this.idTail = idTail;
	}

	public int getRandomBeginTableIndex() {
		return randomBeginTableIndex;
	}

	public void setRandomBeginTableIndex(int randomBeginTableIndex) {
		this.randomBeginTableIndex = randomBeginTableIndex;
	}

	public static TableManager getInstance() {
		return instance;
	}

	private TableManager() {
		tableMap = new ConcurrentHashMap<>();
		resetTableId();
	}

	public TableManager init(int serverId) {
		return this;
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
	 * 获取桌子号
	 */
	public synchronized String getTableId() {
		if (getRandomBeginTableIndex() == 0) {
			setRandomBeginTableIndex(RandomUtils.randomRange(100000) + 100000);
		}
		setRandomBeginTableIndex(getRandomBeginTableIndex() + 1);
		return getIdHead() + getRandomBeginTableIndex() + getIdTail();
	}

	/**
	 * 重置id 头尾拼接字段
	 */
	public synchronized void resetTableId() {
		SimpleDateFormat sp = new SimpleDateFormat("yyMMddmm");
		String head = sp.format(new Date());
		setIdHead(head.substring(0, head.length() / 2));
		setIdTail(head.substring(head.length() / 2));
		setRandomBeginTableIndex(RandomUtils.randomRange(100000) + 100000);
	}
}
