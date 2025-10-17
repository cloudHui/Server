@echo off

:: 定义服务名称和尝试次数
set "server=Room"
set /a count=0

:: 循环检查进程是否完全退出
:check_process
tasklist | findstr /C:"%server%" >nul
if %errorlevel% neq 0 (
    echo %server% 进程已停止
    timeout /t 1 /nobreak >nul

    :: 启动新的服务
    echo 启动 %server% 服务...
    start "" java -jar -Dfile.encoding=UTF-8 -Xms512m -Xmx1g -XX:+UseG1GC "%cd%\%server%.jar" >nul 2>&1

    :: 输出启动完成信息
    echo %server% 服务已启动。
    goto end
)

:: 检查尝试次数是否达到 10 次
if %count% geq 10 (
    echo %server% 服务启动失败,进程未停止,请检查。
    goto end
)

:: 等待 1 秒后重试
timeout /t 1 /nobreak >nul
echo %server% 进程尚未完全退出,等待中... (%count%/10)
set /a count+=1
goto check_process

:end
