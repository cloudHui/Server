package com.gamer.data.mpcserver.core;

import java.io.File;

/**
 * Git MCP 默认仓库路径（由启动参数 --gitRepo= 注入）。
 *
 * @author liuyunhui
 * @date 2026/05/20
 */
public class GitDefaults {

    private final File repoDir;

    /**
     * @param repoDir
     *            Git 仓库根目录（须含 .git）
     */
    public GitDefaults(File repoDir) {
        this.repoDir = repoDir;
    }

    /**
     * @return 仓库目录；未配置时为 null
     */
    public File getRepoDir() {
        return repoDir;
    }
}
