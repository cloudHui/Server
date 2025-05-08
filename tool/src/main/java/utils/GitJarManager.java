package utils;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.other.ExecCommand;

public class GitJarManager {

	private final static Logger logger = LoggerFactory.getLogger(GitJarManager.class);

	private static final String GIT_COMMAND = "git log -1 --pretty=format:\"%H\"";

	private String REV = "";

	private static final String USER_DIR = System.getProperty("user.dir");

	public GitJarManager() {
		List<String> exeCommands = ExecCommand.exeCommand(GIT_COMMAND);
		if (exeCommands.isEmpty()) {
			logger.error("[{} get url version error]", USER_DIR);
			return;
		}
		REV = exeCommands.get(0);
		logger.error("[curr git head:{}]", REV);
	}

	/**
	 * 版本是否更新
	 */
	public boolean checkJarUpdate() {
		if (REV == null || REV.length() <= 0) {
			return false;
		}

		List<String> exeCommands = ExecCommand.exeCommand(GIT_COMMAND);
		if (exeCommands.isEmpty()) {
			logger.error("[checkJarVersionUpdate {} no version ]", USER_DIR);
			return false;
		}

		if (REV.equals(exeCommands.get(0))) {
			return false;
		}
		REV = exeCommands.get(0);
		return true;
	}

	/**
	 * 更新代码
	 */
	public void callBat() {
		String separator = System.getProperty("file.separator");
		ExecCommand.exeBatSh(USER_DIR + separator + "UpdateGit");
	}
}
