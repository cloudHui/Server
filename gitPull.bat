@echo off
chcp 65001
set pull=%~1
cd  %pull%
git pull
goto :eof