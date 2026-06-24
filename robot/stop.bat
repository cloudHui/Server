@echo off

:: 定义服务名称
set "server=Robot"

:: 查找并终止进程
echo 正在停止 %server% 服务...
taskkill /F /FI "WINDOWTITLE eq %server%*" >nul 2>&1
taskkill /F /IM java.exe /FI "MODULES eq %server%" >nul 2>&1

:: 等待进程完全退出
set /a count=0
:check_process
tasklist | findstr /C:"%server%" >nul
if %errorlevel% neq 0 (
    echo %server% 服务已停止。
    goto end
)

:: 检查尝试次数
if %count% geq 10 (
    echo %server% 服务停止失败,请手动检查。
    goto end
)

:: 等待后重试
timeout /t 1 /nobreak >nul
echo 等待 %server% 进程退出... (%count%/10)
set /a count+=1
goto check_process

:end
