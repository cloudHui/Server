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

	private RobotRoomTemplates() {}

	public static boolean isRobotRoom(int roomId) {
		return roomId == MAHJONG_ROOM_ID || roomId == DOU_DIZHU_ROOM_ID;
	}

	public static TableModel mahjong() {
		return TableModelJson.parse("{\"id\":9001,\"type\":1,\"seatNum\":3,\"cardNum\":13,"
				+ "\"exCardNum\":0,\"baseScore\":1,\"maxFan\":16,\"allowChi\":1,"
				+ "\"allowDianPao\":1,\"allowPeng\":1,\"allowGang\":1,\"allowHu\":1,"
				+ "\"allowSevenPairs\":1,\"gameSubType\":0,\"gangScore\":1,"
				+ "\"allowGangMing\":1,\"allowGangAn\":1,\"allowGangBu\":1,"
				+ "\"totalRounds\":4,\"autoNextRound\":1,\"autoPlay\":1,"
				+ "\"waitTimeoutSec\":0,\"waitTimeoutAction\":1}");
	}

	public static TableModel douDiZhu() {
		return TableModelJson.parse("{\"id\":9002,\"type\":2,\"seatNum\":3,\"cardNum\":17,"
				+ "\"exCardNum\":3,\"baseScore\":1,\"maxFan\":16,\"totalRounds\":1,"
				+ "\"autoNextRound\":1,\"autoPlay\":1,\"waitTimeoutSec\":0,"
				+ "\"waitTimeoutAction\":1}");
	}

	public static void register(TableConfigManagerFacade manager) {
		manager.put(mahjong());
		manager.put(douDiZhu());
	}

	/** 让 lobby/game 的配置管理器保持最小耦合。 */
	public interface TableConfigManagerFacade {
		void put(TableModel model);
	}
}
