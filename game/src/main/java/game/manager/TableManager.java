package game.manager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import game.Game;
import game.manager.model.Table;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.utils.RandomUtils;
import utils.utils.TimeUtil;

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

	private TableManager instance = new TableManager();

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

	public TableManager getInstance() {
		return instance;
	}

	public TableManager() {
		tableMap = new ConcurrentHashMap<>();
		resetTableId();
		registerResetTableTask();
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
		Game.getInstance().registerTimer((int) nextZero / 1000,
				(int) TimeUtil.DAY / 1000,
				-1,
				game -> {
					resetTableId();
					return false;
				}, this);
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
