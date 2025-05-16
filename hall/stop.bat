@echo off

:: 定义服务名称和 JAR 文件路径
set "server=Hall"
set "jar_path=%cd%\%server%.jar"

:: 获取所有匹配的进程 ID
for /f "tokens=2" %%i in ('tasklist /FI "IMAGENAME eq java.exe" /FO LIST ^| findstr /C:"%jar_path%"') do (
    echo Stopping %server%.jar (PID: %%i)
    taskkill /PID %%i /F
)

:: 检查是否还有未停止的进程
tasklist /FI "IMAGENAME eq java.exe" /FO LIST | findstr /C:"%jar_path%" >nul
if %errorlevel% equ 0 (
    echo Some processes could not be stopped.
) else (
    echo All %server% processes have been stopped.
)
