FROM ubuntu:latest

RUN apt-get update && apt-get install -y openjdk-17-jre mariadb-server redis-server xvfb
RUN mysql --user=root --execute="CREATE USER jipipe_web IDENTIFIED BY 'jipipe_web'; CREATE DATABASE jipipe_web; GRANT ALL PRIVILEGES ON jipipe_web.* TO jipipe_web; FLUSH PRIVILEGES;"
COPY target /webapp

EXPOSE 80
CMD [ "/use/bin/java", "-jar", "/webapp/ipipe-webapp-growth-assay-analyzer.jar", "--spring.config.location=file:///webapp/application.yaml"]