ping -n 3 127.0.0.1>nul
set cur_dir=%cd%
if exist %cur_dir%\svnUpdate.log (echo update: %date% %time%  >> %cur_dir%\svnUpdate.log) else echo create: %date% %time% > %cur_dir%\svnUpdate.log
svn cleanup
svn update
svn cleanup resources\xml
svn update resources\xml
call StartCurServer.bat
exit
