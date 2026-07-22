# 部署脚本

Windows 推荐从仓库根目录执行 `deploy.bat`，或直接执行 `scripts\ops.bat`。

- `deploy.bat`：唯一的 Windows 部署入口，交互式更新、打包、启动、停止和状态查看
- `deploy.bat deploy`：拉取代码、打包、启动，并询问是否配置 Nginx
- `deploy.bat build`：只打包，不更新代码
- `deploy.bat start`：直接启动现有 `build` 产物
- `deploy.bat stop` / `status`：停止或查看服务
- 本地模式直接访问 `http://127.0.0.1:8081/`，不使用随机路径
- 域名模式会生成 Nginx 反代配置；DNS 解析仍需在域名服务商处完成

Linux 继续使用 `scripts/ops.sh`，两端都只认 `build/<服务>/` 作为运行目录。
