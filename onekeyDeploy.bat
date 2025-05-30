@echo off
chcp 65001

echo ============================================================
echo 欢迎使用一键更新先拉取git 源码 在打包 再更新git jar 包再从 build复制到jar 再提交
echo ============================================================
pause
set if2_java_path="D:\code\Server\build"
set if2_bat_path="D:\code\Server"
rem github
set github="D:\code\ServerJar"
call gitPull.bat %if2_bat_path%
call mvnInstall.bat
call gitPull.bat %github%
echo 完成源码仓库拉取和打包以及 服务包仓库的代码拉取
pause

echo ============================================================
echo 使用注意
echo 1.确保服务器git地址都配置正确。
echo ========================选择服务器版本======================
echo 1.github
echo ============================================================
set /p choice= please choice:
if %choice%==1 (
	set to_path=%github%
)
echo "choice %choice%  to_path %to_path% github %github%"
echo copy to %to_path%
pause
::center
echo copy center
call :process_git_changes "center" "Center" "%to_path%"

::game
echo copy game
call :process_git_changes "game" "Game" "%to_path%"

::gate
echo copy gate
call :process_git_changes "gate" "Gate" "%to_path%"

::hall
echo copy hall
call :process_git_changes "hall" "Hall" "%to_path%"

::room
echo copy room
call :process_git_changes "room" "Room" "%to_path%"

echo 成功复制所有配置文件

:: 切换到Git仓库目录
cd /d %github%

:: 添加所有新文件（未跟踪的文件）
for /f %%i in ('git ls-files --others --exclude-standard') do (
    git add %%i
)

:: 移除所有已从工作目录中删除但未从Git仓库中删除的文件
for /f %%i in ('git ls-files --deleted') do (
    git rm %%i
)

:: 添加所有修改过的文件
git add -u

:: 定义变量
set COMMIT_MSG="Automated commit: Modified, Deleted, and Added files"
:: 提交所有更改
for /f %%i in ('git status --porcelain') do (
    git commit -m %COMMIT_MSG%
    git push origin main
    echo 提交成功：所有修改、删除和新增的文件都已提交。
    goto end
)
echo 没有需要提交的更改。

:end


:: 删除原来的并复制
:process_git_changes
set "REPO_PATH=%~1"
set "BIG_REPO_PATH=%~2"
set "to_path=%~3"
rd /s/q %to_path%\%REPO_PATH%\resources
Xcopy %if2_java_path%\%REPO_PATH%\resources %to_path%\%REPO_PATH%\resources /s /e /y /i

rd /s/q %to_path%\%REPO_PATH%\lib
Xcopy %if2_java_path%\%REPO_PATH%\lib %to_path%\%REPO_PATH%\lib /s /e /y /i

Xcopy %if2_java_path%\%REPO_PATH%\%BIG_REPO_PATH%.jar %to_path%\%BIG_REPO_PATH% /y

Xcopy %if2_bat_path%\%REPO_PATH%\bat\ %to_path%\%REPO_PATH% /y
pause
goto :eof

echo 成功提交所有新增删除和变化文件到git
echo 成功提交所有文件
pause
