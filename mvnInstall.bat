@echo off
chcp 65001
set if2_bat_path=D:\code\Server
cd  %if2_bat_path%
mvn clean compile package install
goto :eof