#!/bin/bash
base_dir=$(dirname $0)/..
LOG_DIR="$base_dir/logs"
CLASSES_DIR="$base_dir/classes"
LIBS_DIR="$base_dir/libs/*"
GC_LOG_FILE_NAME='server-gc.log'
CLASSPATH="$CLASSES_DIR":"$LIBS_DIR"
DBLOGGER_LOG4J_OPTS="-Dlogs.dir=$LOG_DIR"
JVM_OPTS="-Xmx256M "
JVM_PERFORMANCE_OPTS="-server -XX:+UseG1GC -XX:MaxGCPauseMillis=20 -XX:InitiatingHeapOccupancyPercent=35 -XX:+ExplicitGCInvokesConcurrent -Djava.awt.headless=true"
JVM_GC_LOG_OPTS="-Xloggc:$LOG_DIR/$GC_LOG_FILE_NAME -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCDateStamps -XX:+PrintGCTimeStamps -XX:+UseGCLogFileRotation -XX:NumberOfGCLogFiles=10 -XX:GCLogFileSize=100M"
#echo "java $JVM_OPTS $JVM_PERFORMANCE_OPTS $JVM_GC_LOG_OPTS $DBLOGGER_LOG4J_OPTS -cp $CLASSPATH com.gb.dblogger.server.BootStrap"
nohup java -Dspring.profiles.active=development $JVM_OPTS $JVM_PERFORMANCE_OPTS $JVM_GC_LOG_OPTS $DBLOGGER_LOG4J_OPTS -cp $CLASSPATH com.gb.apm.web.ApmWebApplication >/dev/null 2>&1 &