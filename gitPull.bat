@echo off
chcp 65001
set github=%~1
cd  %github%
git pull
goto :eof