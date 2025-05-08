ping -n 3 127.0.0.1>nul
set cur_dir=%cd%
if exist %cur_dir%\gitUpdate.log (echo update: %date% %time%  >> %cur_dir%\gitUpdate.log) else echo create: %date% %time% > %cur_dir%\gitUpdate.log
git pull
sleep 1
call start.bat
exit
