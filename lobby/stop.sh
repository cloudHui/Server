#!/bin/bash

server="Lobby"
jar_path="$(pwd)/${server}.jar"

server_pids=$(ps -ef | grep -v grep | grep "$jar_path" | awk '{print $2}')

for pid in $server_pids; do
    echo "Stopping ${server}.jar (PID: $pid)"
    kill $pid
    if kill -0 $pid 2>/dev/null; then
        echo "Failed to stop PID $pid, trying with -9 signal..."
        kill -9 $pid
    fi
done

remaining_pids=$(ps -ef | grep -v grep | grep "$jar_path" | awk '{print $2}')
if [ -z "$remaining_pids" ]; then
    echo "All ${server} processes have been stopped."
else
    echo "Some processes could not be stopped: $remaining_pids"
fi
