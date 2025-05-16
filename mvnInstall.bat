@echo off
chcp 65001
set install=D:\code\Server
cd  %install%
mvn clean compile package install
goto :eof