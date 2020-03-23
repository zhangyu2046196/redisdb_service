#!/usr/bin/env bash

# author:zhangyu

RUNNING_USER=root
ADATE=`date +%Y%m%d%H%M%S`
APP_NAME=redisdb_cache_service
APP_HOME=/usr/local/redisdb
APP_SERVER_LOG=$APP_HOME/logs

if [ ! -d "$APP_HOME/logs" ];then
  mkdir $APP_SERVER_LOG
fi

LOG_PATH=$APP_SERVER_LOG/$APP_NAME.log
GC_LOG_PATH=$APP_SERVER_LOG/gc-$APP_NAME-$ADATE.log
#JMX监控需用到
#JMX="-Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.port=1091 -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false"
#JVM参数
JVM_OPTS="-Dname=$APP_NAME -Dapp.Home=$APP_HOME -Duser.timezone=Asia/Shanghai -Xmx512m -Xms512m -Xmn256m -Xss512k  -XX:ReservedCodeCacheSize=96m -XX:MaxPermSize=512m -XX:+UseG1GC -XX:MaxGCPauseMillis=10 -XX:GCPauseIntervalMillis=200"


JAR_FILE=$APP_HOME/$APP_NAME-1.0.jar
pid=0
start(){
  checkpid
  if [ ! -n "$pid" ]; then
    JAVA_CMD="nohup java -jar $JVM_OPTS $JAR_FILE --server.contextPath=/  $Monitor_client  > $LOG_PATH 2>&1 &"
    su - $RUNNING_USER -c "$JAVA_CMD"
    echo "---------------------------------"
    echo "启动完成，按CTRL+C退出日志界面即可>>>>>"
    echo "---------------------------------"
    sleep 2s
    tail -f $LOG_PATH
  else
      echo "$APP_NAME is runing PID: $pid"   
  fi

}


status(){
   checkpid
   if [ ! -n "$pid" ]; then
     echo "$APP_NAME not runing"
   else
     echo "$APP_NAME runing PID: $pid"
   fi 
}

checkpid(){
    pid=`ps -ef |grep $JAR_FILE |grep -v grep |awk '{print $2}'`
}

stop(){
    checkpid
    if [ ! -n "$pid" ]; then
     echo "$APP_NAME 服务已经停止"
    else
      echo "$APP_NAME stop..."
      kill -9 $pid
    fi 
	
	while true
	do
        process=`ps -ef |grep $JAR_FILE |grep -v grep |awk '{print $2}'`
        if [ ! -n "$process" ]; then
			echo "$APP_NAME 服务已经停止"
			break
        else
			echo "正在停止 $APP_NAME"
                        sleep 1s
        fi
	done
}

restart(){
    stop 
    sleep 2s
    start
}

case $1 in  
          start) start;;  
          stop)  stop;; 
          restart)  restart;;  
          status)  status;;   
              *)  echo "require start|stop|restart|status"  ;;  
esac 

