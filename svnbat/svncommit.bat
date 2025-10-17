@echo off

:: 定义变量
set REPO_PATH=C:\path\to\your\svn\working\copy
set COMMIT_MSG="Automated commit: Modified, Deleted, and Added files"

:: 切换到SVN工作副本目录
cd /d %REPO_PATH%

:: 自动化处理所有新增文件
:: 查找所有未被版本控制的新文件,并将其添加到版本控制中
for /f "tokens=2 delims= " %%i in ('svn status ^| findstr "^?"') do (
    svn add "%%i"
)

:: 自动化处理所有已删除文件
:: 查找所有已被删除但未标记为删除的文件,并将其标记为删除
for /f "tokens=2 delims= " %%i in ('svn status ^| findstr "^!"') do (
    svn delete "%%i"
)

:: 提交所有更改
svn commit -m %COMMIT_MSG%

echo 所有操作完成并已提交到SVN仓库。
pause
