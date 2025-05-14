@echo off
chcp 65001
echo "先 git update 更新最新代码"
sleep 1
git update

echo "再打包代码"
sleep 1
mvn install
echo "远端代码更新成功 本地代码打包成功"
pause