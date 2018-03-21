#!/bin/bash
cd /validator/
dir=$(ls -1)
for d in $dir ; do
	ln /validator_setup/validator.war /usr/local/tomcat/webapps/$d.war
	echo "<?xml version='1.0' encoding='utf-8'?><Context><WatchedResource>WEB-INF/web.xml</WatchedResource><WatchedResource>/usr/local/tomcat/conf/web.xml</WatchedResource><Environment name=\"spring.config.location\" value=\"/validator/$d/\" type=\"java.lang.String\" override=\"false\"/></Context>" > /usr/local/tomcat/conf/Catalina/localhost/$d.xml
done
ls /usr/local/tomcat/webapps/