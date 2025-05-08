#!/bin/sh
ERR_LOGFILE="gitUpdate.log"
CUR_TIME=`date +'%Y-%m-%d %H-%M-%S'`

if [ -f $ERR_LOGFILE ];then
    echo "update $CUR_TIME" >> ERR_LOGFILE;
else
    echo -e "create: $CUR_TIME\n" > ERR_LOGFILE;
fi

git pull

nohup /usr/local/jdk1.8.0_191/jre/bin/java -jar -server -Dfile.encoding=UTF-8 -Xms512m -Xmx1g -XX:+UseG1GC `pwd`/Room.jar >/dev/null 2>&1 &
