#!/bin/bash
START="/etc/init.d/jetty start"
STOP="/etc/init.d/jetty stop"
LOGFILE="/usr/local/jetty/logs/catalina.out"
SEARCHTERM="Server startup in"

$STOP

#rm -rf /usr/local/tomcat/work/*

$START

#while read LINE; do
#    if [[ $LINE =~ $SEARCHTERM  ]];
#    then
#        echo "Jetty started OK!"
#        break
#    fi
#done < <(tail -f $LOGFILE)

echo "Jetty Restart Complete"