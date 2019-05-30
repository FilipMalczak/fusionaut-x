FROM fusionauth/fusionauth-app:1.3.1

COPY ./build/libs/*-plugin.jar /usr/local/fusionauth/plugins/configurability.jar

CMD /usr/local/fusionauth/fusionauth-app/apache-tomcat/bin/catalina.sh run
