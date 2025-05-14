@echo off
chcp 65001
git update
mvn install
echo "远端代码更新成功 本地代码打包成功"
pause