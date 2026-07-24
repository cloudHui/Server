package model.tablemodel;

/**
 * 系统内置机器人房间模板。
 *
 * <p>机器人房间必须同时被 lobby 和 game 识别，因此模板放在 tool 公共模块，
 * 避免两边各自维护一份规则而产生不一致。</p>
 */
public final class RobotRoomTemplates {
	public static final int MAHJONG_ROOM_ID = 9001;
	public static final int DOU_DIZHU_ROOM_ID = 9002;
	public static final int DOU_DIZHU_ROB_ROOM_ID = 9003;

	private RobotRoomTemplates() {}

	public static boolean isRobotRoom(int roomId) {
		return roomId == MAHJONG_ROOM_ID || roomId == DOU_DIZHU_ROOM_ID || roomId == DOU_DIZHU_ROB_ROOM_ID;
	}

	public static TableModel mahjong() {
		return TableModelJson.parse("{\"id\":9001,\"type\":1,\"seatNum\":3,\"cardNum\":13,"
				+ "\"exCardNum\":0,\"baseScore\":1,\"maxFan\":16,\"allowChi\":1,"
				+ "\"allowDianPao\":1,\"allowPeng\":1,\"allowGang\":1,\"allowHu\":1,"
				+ "\"allowSevenPairs\":1,\"gameSubType\":0,\"gangScore\":1,"
				+ "\"allowGangMing\":1,\"allowGangAn\":1,\"allowGangBu\":1,"
				+ "\"totalRounds\":4,\"autoNextRound\":1,\"autoPlay\":1}");
	}

	public static TableModel douDiZhu() {
		// totalRounds 取较大值：小结算 15 秒后自动连局，地主连庄优先叫牌。
		return TableModelJson.parse("{\"id\":9002,\"type\":2,\"seatNum\":3,\"cardNum\":17,"
				+ "\"exCardNum\":3,\"baseScore\":1,\"maxFan\":16,\"totalRounds\":100,"
				+ "\"autoNextRound\":1,\"autoPlay\":1}");
	}

	/** 电脑快速房间：叫地主后立即进入逆时针抢/再抢。 */
	public static TableModel douDiZhuRob() {
		return TableModelJson.parse("{\"id\":9003,\"type\":2,\"seatNum\":3,\"cardNum\":17,\"exCardNum\":3,\"baseScore\":1,\"maxFan\":16,\"gameSubType\":1,\"totalRounds\":100,\"autoNextRound\":1,\"autoPlay\":1}");
	}

	public static void register(TableConfigManagerFacade manager) {
		manager.put(mahjong());
		manager.put(douDiZhu());
		manager.put(douDiZhuRob());
	}

	/** 让 lobby/game 的配置管理器保持最小耦合。 */
	public interface TableConfigManagerFacade {
		void put(TableModel model);
	}
}
