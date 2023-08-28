package utils;

import java.util.List;

import utils.utils.ExecCommand;
import utils.utils.StringUtils;

public class SvnManager {

	private static SvnManager instance;

	public static SvnManager getInstance() {
		return instance;
	}

	private String REV = "Revision: ";

	public SvnManager() {
		String path = System.getProperty("user.dir");
		List<String> exeCommands = ExecCommand.exeCommand("svn info " + path);
		jarUrl = ExecCommand.getExeCommandResult(exeCommands, "Url: ");
		jarLastVersion = ExecCommand.getExeCommandResult(exeCommands, REV);
	}

	/**
	 * 运行包地址
	 */
	private String jarUrl;

	/**
	 * 上一次检测版本
	 */
	private String jarLastVersion;

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
