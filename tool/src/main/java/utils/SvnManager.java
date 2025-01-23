package utils;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.utils.ExecCommand;
import utils.utils.StringUtils;

public class SvnManager {

	private final static Logger logger = LoggerFactory.getLogger(SvnManager.class);


	private final String REV = "Revision: ";

	public SvnManager() {
		String path = System.getProperty("user.dir");
		List<String> exeCommands = ExecCommand.exeCommand("svn info " + path);
		jarUrl = ExecCommand.getExeCommandResult(exeCommands, "Url: ");
		jarLastVersion = ExecCommand.getExeCommandResult(exeCommands, REV);
		if (jarLastVersion.length() == 0 || jarUrl.length() == 0) {
			logger.error("get url version error");
		}
	}

	/**
	 * 运行包地址
	 */
	private final String jarUrl;

	/**
	 * 上一次检测版本
	 */
	private final String jarLastVersion;

	public String getJarUrl() {
		return jarUrl;
	}

	public String getJarLastVersion() {
		return jarLastVersion;
	}

	/**
	 * 获取当前版本
	 */
	public String getVersion() {
		return ExecCommand.exeCommand(jarUrl, REV);
	}

	/**
	 * 版本是否更新
	 */
	public boolean checkJarVersionUpdate() {
		String jarLastVersion = getJarLastVersion();
		if (jarLastVersion == null || jarLastVersion.length() <= 0) {
			return false;
		}
		String currVersion = getVersion();

		return StringUtils.versionBigger(jarLastVersion, currVersion);
	}

	/**
	 * 更新代码
	 */
	public void jarUpdate() {
		ExecCommand.exeCommand("svn update " + getJarUrl());
	}

	/**
	 * 更新代码
	 */
	public void callBat() {
		String path = System.getProperty("user.dir");
		String separator = System.getProperty("file.separator");
		String osName = System.getProperty("os.name");
		if (osName.contains("Windows")) {
			ExecCommand.exeBatSh(path + separator + "Update.bat");
		} else {
			ExecCommand.exeBatSh(path + separator + " Update.sh");
		}
	}
}
