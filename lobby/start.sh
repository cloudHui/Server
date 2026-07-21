#!/bin/sh

server="Lobby"
count=0

while true; do
    servers=$(ps -ef | grep "${server}" | grep -v grep | wc -l)

    if [ "$servers" -le 1 ] ;then
        echo "${server} 进程已停止"
        sleep 1
        echo "启动 ${server} 服务..."
        nohup java -jar -Dfile.encoding=UTF-8 -Xms512m -Xmx1g -XX:+UseG1GC "$(pwd)/${server}.jar" >/dev/null 2>&1 &
        echo "${server} 服务已启动。"
        break
    elif  [ "$count" -ge 10 ]; then
        echo "${server} 服务启动失败进程未停止,请检查。"
        break
    else
        sleep 1
        echo "${server} 进程尚未完全退出,等待中... (${count}/10)"
        count=$((count + 1))
    fi
done
