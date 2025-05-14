@echo off
chcp 65001
set if2_java_path=..\Server\build
set if2_bat_path=..\Server
::github
set github=D:\code\git\ServerJar

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

echo copy to %to_path%

::center
echo copy center

rd /s/q %to_path%\center\resources
Xcopy %if2_java_path%\center\resources %to_path%\center\resources /s /e /y /i

rd /s/q %to_path%\center\lib
Xcopy %if2_java_path%\center\lib %to_path%\center\lib /s /e /y /i

Xcopy %if2_java_path%\center\Center.jar %to_path%\center /y

Xcopy %if2_bat_path%\center\bat\ %to_path%\center /y

::game
echo copy game

rd /s/q %to_path%\game\resources
Xcopy %if2_java_path%\game\resources %to_path%\game\resources /s /e /y /i

rd /s/q %to_path%\game\lib
Xcopy %if2_java_path%\game\lib %to_path%\game\lib /s /e /y /i

Xcopy %if2_java_path%\game\Game.jar %to_path%\game /y

Xcopy %if2_bat_path%\game\bat\ %to_path%\game /y

::gate
echo copy gate

rd /s/q %to_path%\gate\resources
Xcopy %if2_java_path%\gate\resources %to_path%\gate\resources /s /e /y /i

rd /s/q %to_path%\gate\lib
Xcopy %if2_java_path%\gate\lib %to_path%\gate\lib /s /e /y /i

Xcopy %if2_java_path%\gate\Gate.jar %to_path%\gate /y

Xcopy %if2_bat_path%\gate\bat\ %to_path%\gate /y

::hall
echo copy hall

rd /s/q %to_path%\hall\resources
Xcopy %if2_java_path%\hall\resources %to_path%\hall\resources /s /e /y /i

rd /s/q %to_path%\hall\lib
Xcopy %if2_java_path%\hall\lib %to_path%\hall\lib /s /e /y /i

Xcopy %if2_java_path%\hall\Hall.jar %to_path%\hall /y

Xcopy %if2_bat_path%\hall\bat\ %to_path%\hall /y

::room
echo copy room

rd /s/q %to_path%\room\resources
Xcopy %if2_java_path%\room\resources %to_path%\room\resources /s /e /y /i

rd /s/q %to_path%\room\lib
Xcopy %if2_java_path%\room\lib %to_path%\room\lib /s /e /y /i

Xcopy %if2_java_path%\room\Room.jar %to_path%\Room /y

Xcopy %if2_bat_path%\room\bat\ %to_path%\room /y

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

echo 成功提交所有文件
pause
