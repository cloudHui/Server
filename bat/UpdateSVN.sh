#!/bin/sh
ERR_LOGFILE="svnUpdate.log"
CUR_TIME=`date +'%Y-%m-%d %H-%M-%S'`

if [ -f $ERR_LOGFILE ];then
    echo "update $CUR_TIME" >> svnUpdate.log;
else
    echo -e "create: $CUR_TIME\n" > svnUpdate.log;
fi

svn cleanup
svn update
svn cleanup resources/xml
svn update resources/xml

nohup /usr/local/jdk1.8.0_191/jre/bin/java -jar -server -Dfile.encoding=UTF-8 -Xms512m -Xmx1g -XX:+UseG1GC `pwd`/if2-gameServer-1.0-SNAPSHOT.jar >/dev/null 2>&1 &
