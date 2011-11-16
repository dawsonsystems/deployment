#!/bin/bash
START="/etc/init.d/tomcat7 start"
STOP="/etc/init.d/tomcat7 stop"
LOGFILE="/var/lib/tomcat7/logs/catalina.out"
SEARCHTERM="Server startup in"

$STOP

rm -rf /var/lib/tomcat7/work/*

$START

while read LINE; do
    if [[ $LINE =~ $SEARCHTERM  ]];
    then
        echo "Tomcat7 started OK!"
        break
    fi
done < <(tail -f $LOGFILE)

echo "Tomcat Restart Complete"
