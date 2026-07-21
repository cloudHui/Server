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
			return m;
		} catch (Exception e) {
			logger.warn("解析自定义 TableModel 失败: {}", e.getMessage());
			return null;
		}
	}

	public static String toJson(TableModel m) {
		StringBuilder sb = new StringBuilder();
		sb.append('{');
		sb.append("\"id\":").append(m.getId()).append(',');
		sb.append("\"type\":").append(m.getType()).append(',');
		sb.append("\"seatNum\":").append(m.getSeatNum()).append(',');
		sb.append("\"cardNum\":").append(m.getCardNum()).append(',');
		sb.append("\"exCardNum\":").append(m.getExCardNum()).append(',');
		sb.append("\"baseScore\":").append(m.getBaseScore()).append(',');
		sb.append("\"maxFan\":").append(m.getMaxFan()).append(',');
		sb.append("\"allowChi\":").append(m.getAllowChi()).append(',');
		sb.append("\"allowDianPao\":").append(m.getAllowDianPao()).append(',');
		sb.append("\"allowPeng\":").append(m.getAllowPeng()).append(',');
		sb.append("\"allowGang\":").append(m.getAllowGang()).append(',');
		sb.append("\"allowHu\":").append(m.getAllowHu()).append(',');
		sb.append("\"allowSevenPairs\":").append(m.getAllowSevenPairs()).append(',');
		sb.append("\"gameSubType\":").append(m.getGameSubType()).append(',');
		sb.append("\"gangScore\":").append(m.getGangScore()).append(',');
		sb.append("\"allowGangMing\":").append(m.getAllowGangMing()).append(',');
		sb.append("\"allowGangAn\":").append(m.getAllowGangAn()).append(',');
		sb.append("\"allowGangBu\":").append(m.getAllowGangBu()).append(',');
		sb.append("\"totalRounds\":").append(m.getTotalRounds()).append(',');
		sb.append("\"autoNextRound\":").append(m.getAutoNextRound()).append(',');
		sb.append("\"autoPlay\":").append(m.getAutoPlay());
		sb.append('}');
		return sb.toString();
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
