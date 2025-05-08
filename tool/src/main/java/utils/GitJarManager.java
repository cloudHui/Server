package utils;

import java.io.File;
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

	/**
	 * git更新版本
	 */
	private String REV = "";

	private static final String USER_DIR = System.getProperty("user.dir");

	private Map<File, Long> fileMap;

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
	 * 版本是否更新
	 */
	public void checkJarUpdate() {
		logger.error("checkJarUpdate:{} ", USER_DIR);
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

		//获取新的jar 目录下的文件
		Map<File, Long> newMap = new HashMap<>();
		DirectoryScanner.listFiles(USER_DIR, newMap);
		//比较数量不一致直接更新
		if (newMap.size() != fileMap.size()) {
			fileMap = newMap;
			callBat();
			System.exit(0);
		} else {
			//数量一致 看是否只有配置更新了 配置更新值只更新配置 否则重启服务
			boolean xlsxChange = false;
			for (Map.Entry<File, Long> entry : newMap.entrySet()) {
				if (!fileMap.get(entry.getKey()).equals(entry.getValue())) {
					if (entry.getKey().getName().contains(".xlsx")) {
						xlsxChange = true;
					} else if (entry.getKey().getName().contains(".jar")) {
						callBat();
						System.exit(0);
					}
				}
			}

			if (xlsxChange) {
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
