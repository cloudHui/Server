#!/bin/sh

server="Gate"
count=0

# 循环检查进程是否完全退出
while true; do
    # 获取名为 "${server}" 的进程数量（排除 grep 自身）
    servers=$(ps -ef | grep "${server}" | grep -v grep | wc -l)

    # 如果进程数为 1（表示只有 grep 进程）或尝试次数达到 10 次，则退出循环
    if [ "$servers" -le 1 ] ;then
        echo "${server} 进程已停止"
        sleep 1
        # 启动新的服务
        echo "启动 ${server} 服务..."
        nohup java -jar -Dfile.encoding=UTF-8 -Xms512m -Xmx1g -XX:+UseG1GC "$(pwd)/${server}.jar" >/dev/null 2>&1 &
        # 输出启动完成信息
        echo "${server} 服务已启动。"
        break
    elif  [ "$count" -ge 10 ]; then
         # 输出启动完成信息
        echo "${server} 服务启动失败进程未停止,请检查。"
        break
    else
        # 等待 1 秒后重试
        sleep 1
        echo "${server} 进程尚未完全退出，等待中... (${count}/10)"
        count=$((count + 1))
    fi
done

