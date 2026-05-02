package game.manager.table.ddz;

/**
 * 与 {@link proto.ConstProto.Operation} 数值对齐（见 const.proto 9-11），服务端解析用 {@link proto.GameProto.OpInfo#getChoiceValue()}。
 */
public final class DdzBidOpcodes {

	public static final int CALL_SCORE_1 = 9;
	public static final int CALL_SCORE_2 = 10;
	public static final int CALL_SCORE_3 = 11;

	private DdzBidOpcodes() {
	}

	public static int callScoreFromChoiceValue(int choiceValue) {
		switch (choiceValue) {
			case CALL_SCORE_1:
				return 1;
			case CALL_SCORE_2:
				return 2;
			case CALL_SCORE_3:
				return 3;
			default:
				return 0;
		}
	}

	public static boolean isCallScore(int choiceValue) {
		return choiceValue == CALL_SCORE_1 || choiceValue == CALL_SCORE_2 || choiceValue == CALL_SCORE_3;
	}
}
