@echo off
setlocal EnableExtensions EnableDelayedExpansion
chcp 65001 >nul

set "ROOT=%~dp0.."
for %%I in ("%ROOT%") do set "ROOT=%%~fI"
set "BUILD=%ROOT%\build"
set "LOGS=%ROOT%\logs"
set "MODE=local"
set "DOMAIN="
set "NGINX_HOME="

if /I "%~1"=="build" goto build
if /I "%~1"=="start" goto start
if /I "%~1"=="stop" goto stop
if /I "%~1"=="status" goto status
if /I "%~1"=="update" goto update
if /I "%~1"=="deploy" goto deploy
if /I "%~1"=="help" goto help
if /I "%~1"=="-h" goto help
if /I "%~1"=="--help" goto help
goto menu

:menu
echo.
echo ============================================================
echo Server Windows 部署入口
echo ============================================================
echo 1. 更新代码、打包并启动
echo 2. 打包并启动（不更新代码）
echo 3. 仅启动
echo 4. 停止全部服务
echo 5. 查看状态
echo 0. 退出
echo.
set "CHOICE="
set /p "CHOICE=请选择: "
if "%CHOICE%"=="1" goto deploy
if "%CHOICE%"=="2" goto build_start
if "%CHOICE%"=="3" goto start
if "%CHOICE%"=="4" goto stop
if "%CHOICE%"=="5" goto status
if "%CHOICE%"=="0" exit /b 0
echo 无效选项。
goto menu

:deploy
call :update_code || exit /b 1
call :build_code || exit /b 1
goto configure_and_start

:build_start
call :build_code || exit /b 1
goto configure_and_start

:update
call :update_code
exit /b %errorlevel%

:build
call :build_code
exit /b %errorlevel%

:start
call :choose_access_mode
call :start_all
if /I "%MODE%"=="domain" call :configure_nginx
goto done

:configure_and_start
call :choose_access_mode
call :start_all
if /I "%MODE%"=="domain" call :configure_nginx
goto done

:stop
call :stop_all
goto done

:status
call :status_all
goto done

:update_code
cd /d "%ROOT%"
echo.
echo [更新] 检查本地修改...
for /f "delims=" %%I in ('git status --porcelain') do set "DIRTY=1"
if defined DIRTY (
  echo 检测到未提交修改，为避免覆盖，本次不执行 git pull。
  echo 请先提交或暂存修改后重试。
  exit /b 1
)
git pull --ff-only
if errorlevel 1 (
  echo git pull 失败，请检查网络或分支冲突。
  exit /b 1
)
exit /b 0

:build_code
cd /d "%ROOT%"
where mvn >nul 2>&1 || (echo 未找到 Maven，请先配置 PATH。& exit /b 1)
echo.
echo [打包] Maven install（跳过测试，跳过 mcp、sp）...
mvn -q install -DskipTests -pl ^!mcp,^!sp
if errorlevel 1 (echo Maven 打包失败。& exit /b 1)

for %%S in (center gate lobby game web) do if not exist "%BUILD%\%%S" mkdir "%BUILD%\%%S"
if not exist "%BUILD%\center\Center.jar" (echo 缺少 build\center\Center.jar& exit /b 1)
if not exist "%BUILD%\gate\Gate.jar" (echo 缺少 build\gate\Gate.jar& exit /b 1)
if not exist "%BUILD%\lobby\Lobby.jar" (echo 缺少 build\lobby\Lobby.jar& exit /b 1)
if not exist "%BUILD%\game\Game.jar" (echo 缺少 build\game\Game.jar& exit /b 1)
if not exist "%BUILD%\web\Web.jar" (echo 缺少 build\web\Web.jar& exit /b 1)
call :sync_proto
echo [打包] 完成，产物位于 %BUILD%。
exit /b 0

:sync_proto
if not exist "%ROOT%\proto\target\proto-1.0-SNAPSHOT.jar" exit /b 0
for %%S in (center gate lobby game) do (
  if not exist "%BUILD%\%%S\lib" mkdir "%BUILD%\%%S\lib"
  copy /y "%ROOT%\proto\target\proto-1.0-SNAPSHOT.jar" "%BUILD%\%%S\lib\proto-1.0-SNAPSHOT.jar" >nul
)
exit /b 0

:choose_access_mode
set "MODE=local"
set "DOMAIN="
echo.
set "USE_DOMAIN="
set /p "USE_DOMAIN=是否配置域名/Nginx？(Y/N，默认 N): "
if /I not "%USE_DOMAIN%"=="Y" (
  echo 使用本地模式：http://127.0.0.1:8081/
  exit /b 0
)
set /p "DOMAIN=请输入域名（例如 api.example.com）: "
if "%DOMAIN%"=="" (
  echo 未输入域名，改用本地模式。
  exit /b 0
)
set "MODE=domain"
exit /b 0

:start_all
where java >nul 2>&1 || (echo 未找到 Java，请先配置 PATH。& exit /b 1)
call :start_one center Center 64m || exit /b 1
call :start_one gate Gate 96m || exit /b 1
call :start_one lobby Lobby 128m || exit /b 1
call :start_one game Game 128m || exit /b 1
call :start_one web Web 192m || exit /b 1
if /I "%MODE%"=="domain" (echo Web 地址：http://%DOMAIN%/  ) else (echo Web 地址：http://127.0.0.1:8081/)
exit /b 0

:start_one
set "SVC=%~1"
set "NAME=%~2"
set "HEAP=%~3"
if not exist "%BUILD%\%SVC%\%NAME%.jar" (
  echo [%NAME%] 找不到 JAR，请先执行 deploy.bat build。
  exit /b 1
)
call :find_pid "%NAME%.jar" PID
if defined PID (
  echo [%NAME%] 已在运行，PID !PID!。
  exit /b 0
)
if not exist "%LOGS%\%SVC%" mkdir "%LOGS%\%SVC%"
set "CTX=/"
start "%NAME%" /b cmd /c "cd /d "%ROOT%" ^&^& java -Dfile.encoding=UTF-8 -DLOG_HOME="%LOGS%" -Dserver.servlet.context-path=!CTX! -Xms%HEAP% -Xmx%HEAP% -XX:+UseG1GC -jar "%BUILD%\%SVC%\%NAME%.jar" >> "%LOGS%\%SVC%\console.out" 2>&1"
timeout /t 2 /nobreak >nul
call :find_pid "%NAME%.jar" PID
if not defined PID (echo [%NAME%] 启动失败，请检查 %LOGS%\%SVC%\console.out。& exit /b 1)
echo [%NAME%] 已启动，PID !PID!，内存 %HEAP%。
exit /b 0

:find_pid
set "%~2="
for /f "usebackq delims=" %%I in (`powershell -NoProfile -ExecutionPolicy Bypass -Command "$p=Get-CimInstance Win32_Process -Filter 'Name = ''java.exe''' | Where-Object { $_.CommandLine -like ('*' + '%~1' + '*') } | Select-Object -First 1 -ExpandProperty ProcessId; if ($p) { $p }"`) do set "%~2=%%I"
exit /b 0

:stop_all
for %%I in (Center Gate Lobby Game Web) do call :stop_one %%I
exit /b 0

:stop_one
set "PID="
call :find_pid "%~1.jar" PID
if not defined PID (echo [%~1] 未运行。& exit /b 0)
taskkill /PID !PID! /T /F >nul
echo [%~1] 已停止。
exit /b 0

:status_all
for %%I in (Center Gate Lobby Game Web) do (
  set "PID="
  call :find_pid "%%I.jar" PID
  if defined PID (echo [%%I] running PID !PID!) else (echo [%%I] stopped)
)
echo 本地地址：http://127.0.0.1:8081/
exit /b 0

:configure_nginx
echo.
set /p "NGINX_HOME=请输入 Nginx 目录（例如 C:\nginx，留空则只生成配置）: "
if "%NGINX_HOME%"=="" set "NGINX_HOME=%ROOT%\scripts\nginx\generated"
if not exist "%NGINX_HOME%\conf\conf.d" mkdir "%NGINX_HOME%\conf\conf.d" 2>nul
if not exist "%NGINX_HOME%\conf\conf.d" (
  mkdir "%NGINX_HOME%" 2>nul
  set "NGINX_CONF=%NGINX_HOME%\server-%DOMAIN%.conf"
) else set "NGINX_CONF=%NGINX_HOME%\conf\conf.d\server-%DOMAIN%.conf"
>"%NGINX_CONF%" echo server {
>>"%NGINX_CONF%" echo     listen 80;
>>"%NGINX_CONF%" echo     server_name %DOMAIN%;
>>"%NGINX_CONF%" echo     location / {
>>"%NGINX_CONF%" echo         proxy_pass http://127.0.0.1:8081;
>>"%NGINX_CONF%" echo         proxy_http_version 1.1;
>>"%NGINX_CONF%" echo         proxy_set_header Host %%host;
>>"%NGINX_CONF%" echo         proxy_set_header X-Real-IP %%remote_addr;
>>"%NGINX_CONF%" echo         proxy_set_header X-Forwarded-For %%proxy_add_x_forwarded_for;
>>"%NGINX_CONF%" echo         proxy_set_header Upgrade %%http_upgrade;
>>"%NGINX_CONF%" echo         proxy_set_header Connection "upgrade";
>>"%NGINX_CONF%" echo     }
>>"%NGINX_CONF%" echo }
echo Nginx 配置已生成：%NGINX_CONF%
if exist "%NGINX_HOME%\nginx.exe" (
  "%NGINX_HOME%\nginx.exe" -t
  if not errorlevel 1 "%NGINX_HOME%\nginx.exe" -s reload
) else echo 未找到 nginx.exe，请将配置加入 Nginx conf.d 并手动 reload。
echo 请确认 DNS 已将 %DOMAIN% 指向当前服务器。
exit /b 0

:help
echo 用法：deploy.bat [deploy^|build^|start^|stop^|status^|update]
echo 不带参数进入交互菜单；deploy 会更新代码、打包、启动并询问 Nginx。
exit /b 0

:done
echo.
pause
exit /b 0
