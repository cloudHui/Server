package model.tablemodel;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 极简 TableModel JSON 解析（自定义创房），避免引入额外依赖。
 * 支持扁平字段：id,type,seatNum,cardNum,exCardNum,baseScore,maxFan,allowChi,...
 */
public final class TableModelJson {
	private static final Logger logger = LoggerFactory.getLogger(TableModelJson.class);

	private TableModelJson() {}

	public static TableModel parse(String json) {
		if (json == null || json.trim().isEmpty()) return null;
		try {
			TableModel m = new TableModel();
			m.setId(intVal(json, "id", 0));
			m.setType(intVal(json, "type", 1));
			m.setSeatNum(intVal(json, "seatNum", m.getType() == 2 ? 3 : 4));
			m.setCardNum(intVal(json, "cardNum", m.getType() == 2 ? 17 : 13));
			m.setExCardNum(intVal(json, "exCardNum", m.getType() == 2 ? 3 : 0));
			m.setBaseScore(intVal(json, "baseScore", 1));
			m.setMaxFan(intVal(json, "maxFan", 16));
			m.setAllowChi(intVal(json, "allowChi", 1));
			m.setAllowDianPao(intVal(json, "allowDianPao", 1));
			m.setAllowPeng(intVal(json, "allowPeng", 1));
			m.setAllowGang(intVal(json, "allowGang", 1));
			m.setAllowHu(intVal(json, "allowHu", 1));
			m.setAllowSevenPairs(intVal(json, "allowSevenPairs", 1));
			m.setGameSubType(intVal(json, "gameSubType", 0));
			m.setGangScore(intVal(json, "gangScore", 1));
			m.setAllowGangMing(intVal(json, "allowGangMing", 1));
			m.setAllowGangAn(intVal(json, "allowGangAn", 1));
			m.setAllowGangBu(intVal(json, "allowGangBu", 1));
			m.setTotalRounds(intVal(json, "totalRounds", 4));
			m.setAutoNextRound(intVal(json, "autoNextRound", 0));
			m.setAutoPlay(intVal(json, "autoPlay", 0));
			m.setWaitTimeoutSec(intVal(json, "waitTimeoutSec", 120));
			m.setWaitTimeoutAction(actionVal(json, "waitTimeoutAction", 0));
			return m;
		} catch (Exception e) {
			logger.warn("解析自定义 TableModel 失败: {}", e.getMessage());
			return null;
		}
	}

	public static String toJson(TableModel m) {
        return '{' +
                "\"id\":" + m.getId() + ',' +
                "\"type\":" + m.getType() + ',' +
                "\"seatNum\":" + m.getSeatNum() + ',' +
                "\"cardNum\":" + m.getCardNum() + ',' +
                "\"exCardNum\":" + m.getExCardNum() + ',' +
                "\"baseScore\":" + m.getBaseScore() + ',' +
                "\"maxFan\":" + m.getMaxFan() + ',' +
                "\"allowChi\":" + m.getAllowChi() + ',' +
                "\"allowDianPao\":" + m.getAllowDianPao() + ',' +
                "\"allowPeng\":" + m.getAllowPeng() + ',' +
                "\"allowGang\":" + m.getAllowGang() + ',' +
                "\"allowHu\":" + m.getAllowHu() + ',' +
                "\"allowSevenPairs\":" + m.getAllowSevenPairs() + ',' +
                "\"gameSubType\":" + m.getGameSubType() + ',' +
                "\"gangScore\":" + m.getGangScore() + ',' +
                "\"allowGangMing\":" + m.getAllowGangMing() + ',' +
                "\"allowGangAn\":" + m.getAllowGangAn() + ',' +
                "\"allowGangBu\":" + m.getAllowGangBu() + ',' +
                "\"totalRounds\":" + m.getTotalRounds() + ',' +
                "\"autoNextRound\":" + m.getAutoNextRound() + ',' +
                "\"autoPlay\":" + m.getAutoPlay() + ',' +
                "\"waitTimeoutSec\":" + m.getWaitTimeoutSec() + ',' +
                "\"waitTimeoutAction\":" + m.getWaitTimeoutAction() + ',' +
                '}';
	}

	/** 0=dissolve / 1=fillRobot，兼容数字或字符串 */
	private static int actionVal(String json, String key, int def) {
		String pattern = "\"" + key + "\"";
		int idx = json.indexOf(pattern);
		if (idx < 0) return def;
		int colon = json.indexOf(':', idx + pattern.length());
		if (colon < 0) return def;
		int i = colon + 1;
		while (i < json.length() && Character.isWhitespace(json.charAt(i))) i++;
		if (i < json.length() && json.charAt(i) == '"') {
			int end = json.indexOf('"', i + 1);
			if (end < 0) return def;
			String s = json.substring(i + 1, end).trim().toLowerCase();
			if ("fillrobot".equals(s) || "1".equals(s)) return 1;
			if ("dissolve".equals(s) || "0".equals(s)) return 0;
			return def;
		}
		return intVal(json, key, def);
	}

	private static int intVal(String json, String key, int def) {
		String pattern = "\"" + key + "\"";
		int idx = json.indexOf(pattern);
		if (idx < 0) return def;
		int colon = json.indexOf(':', idx + pattern.length());
		if (colon < 0) return def;
		int i = colon + 1;
		while (i < json.length() && Character.isWhitespace(json.charAt(i))) i++;
		int j = i;
		if (j < json.length() && json.charAt(j) == '-') j++;
		while (j < json.length() && Character.isDigit(json.charAt(j))) j++;
		if (j == i || (j == i + 1 && json.charAt(i) == '-')) return def;
		try {
			return Integer.parseInt(json.substring(i, j));
		} catch (NumberFormatException e) {
			return def;
		}
	}
}
