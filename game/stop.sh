#!/bin/bash

# 定义服务名称和 JAR 文件路径
server="Game"
jar_path="$(pwd)/${server}.jar"

# 获取所有匹配的进程 ID
server_pids=$(ps -ef | grep -v grep | grep "$jar_path" | awk '{print $2}')

# 遍历所有匹配的进程 ID
for pid in $server_pids; do
    echo "Stopping ${server}.jar (PID: $pid)"
    kill $pid

    # 检查是否成功杀死进程
    if kill -0 $pid 2>/dev/null; then
        echo "Failed to stop PID $pid, trying with -9 signal..."
        kill -9 $pid
    fi
done

# 确保所有进程已停止
remaining_pids=$(ps -ef | grep -v grep | grep "$jar_path" | awk '{print $2}')
if [ -z "$remaining_pids" ]; then
    echo "All ${server} processes have been stopped."
else
    echo "Some processes could not be stopped: $remaining_pids"
fi
