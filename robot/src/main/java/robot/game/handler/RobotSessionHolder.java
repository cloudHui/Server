package robot.game.handler;

import robot.game.RobotGameSession;

/**
 * Robot会话持有者（单例）
 * 管理当前Robot的游戏会话实例
 */
public class RobotSessionHolder {

	private static final RobotGameSession SESSION = new RobotGameSession();

	/** 获取当前会话 */
	public static RobotGameSession getSession() {
		return SESSION;
	}
}
