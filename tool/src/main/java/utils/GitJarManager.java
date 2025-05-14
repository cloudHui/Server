package utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.other.DirectoryScanner;
import utils.other.ExecCommand;

public class GitJarManager {

	private final static Logger logger = LoggerFactory.getLogger(GitJarManager.class);

	private static final String GIT_COMMAND = "git log -1 --pretty=format:\"%H\"";

	//获取远端最新版本
	private static final String GIT_REMOTE = "git ls-remote origin main";

	/**
	 * git更新版本
	 */
	private String REV = "";

	private static final String USER_DIR = System.getProperty("user.dir");

	/**
	 * 文件名 和文件修改时间
	 */
	private Map<String, Long> fileMap;

	public GitJarManager() {
		fileMap = new HashMap<>();
		DirectoryScanner.listFiles(USER_DIR, fileMap);
		List<String> exeCommands = ExecCommand.exeCommand(GIT_COMMAND);
		if (exeCommands.isEmpty()) {
			logger.error("[{} get url version error]", USER_DIR);
			return;
		}
		REV = exeCommands.get(0);
		logger.error("[fileMap:{} curr git head:{}]", fileMap.size(), REV);
	}


	/**
	 * 获取最新的版本号
	 */
	private String getGitNewestVersion() {
		List<String> list = ExecCommand.exeCommand(GIT_REMOTE);
		if (list.isEmpty()) {
			try {
				throw new Exception("getGitNewestVersion message null " + GIT_REMOTE);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return list.get(0).split("refs")[0].trim();
	}

	/**
	 * 版本是否更新
	 */
	public void checkJarUpdate() {
		logger.error("checkJarUpdate:{} REV: {}", USER_DIR, REV);
		if (REV == null || REV.length() <= 0) {
			logger.error("REV == null || REV.length() <= 0");
			return;
		}

		List<String> exeCommands = ExecCommand.exeCommand(GIT_COMMAND);
		if (exeCommands.isEmpty()) {
			logger.error("[checkJarVersionUpdate {} no version data]", USER_DIR);
			return;
		}

		//校验最新版本和旧版本是否一致
		if (REV.equals(exeCommands.get(0))) {
			return;
		}
		REV = exeCommands.get(0);
		//检测更新或者重启
		checkUpRestart();
	}

	/**
	 * 检测更新或者重启
	 */
	private void checkUpRestart() {
		//获取新的jar 目录下的文件
		Map<String, Long> newMap = new HashMap<>();
		DirectoryScanner.listFiles(USER_DIR, newMap);
		//比较数量不一致直接更新
		if (newMap.size() != fileMap.size()) {
			fileMap = newMap;
			logger.error("checkUpRestart restart: {}", REV);
			callBat();
			System.exit(0);
		} else {
			//数量一致 看是否只有配置更新了 配置更新值只更新配置 否则重启服务
			boolean xlsxChange = false;
			for (Map.Entry<String, Long> entry : newMap.entrySet()) {
				if (!fileMap.get(entry.getKey()).equals(entry.getValue())) {
					if (entry.getKey().contains(".xlsx")) {
						xlsxChange = true;
					} else if (entry.getKey().contains(".jar")) {
						logger.error("checkUpRestart restart: {}", REV);
						callBat();
						System.exit(0);
					}
				}
			}

			if (xlsxChange) {
				logger.error("checkUpRestart update: {}", REV);
				callBat();
			}
		}
	}

	/**
	 * 更新代码
	 */
	public void callBat() {
		String separator = System.getProperty("file.separator");
		ExecCommand.exeBatSh(USER_DIR + separator + "UpdateGit");
	}
}
