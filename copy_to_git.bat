@echo off
chcp 65001
set if2_java_path=..\Server\build

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


::game
echo copy game

rd /s/q %to_path%\game\resources
Xcopy %if2_java_path%\game\resources %to_path%\game\resources /s /e /y /i

rd /s/q %to_path%\game\lib
Xcopy %if2_java_path%\game\lib %to_path%\game\lib /s /e /y /i

Xcopy %if2_java_path%\game\Game.jar %to_path%\game /y

::gate
echo copy gate

rd /s/q %to_path%\gate\resources
Xcopy %if2_java_path%\gate\resources %to_path%\gate\resources /s /e /y /i

rd /s/q %to_path%\gate\lib
Xcopy %if2_java_path%\gate\lib %to_path%\gate\lib /s /e /y /i

Xcopy %if2_java_path%\gate\Gate.jar %to_path%\gate /y

::hall
echo copy hall

rd /s/q %to_path%\hall\resources
Xcopy %if2_java_path%\hall\resources %to_path%\hall\resources /s /e /y /i

rd /s/q %to_path%\hall\lib
Xcopy %if2_java_path%\hall\lib %to_path%\hall\lib /s /e /y /i

Xcopy %if2_java_path%\hall\Hall.jar %to_path%\hall /y

::room
echo copy room

rd /s/q %to_path%\room\resources
Xcopy %if2_java_path%\room\resources %to_path%\room\resources /s /e /y /i

rd /s/q %to_path%\room\lib
Xcopy %if2_java_path%\room\lib %to_path%\room\lib /s /e /y /i

Xcopy %if2_java_path%\room\Room.jar %to_path%\Room /y

echo 成功处理所有配置文件
pause