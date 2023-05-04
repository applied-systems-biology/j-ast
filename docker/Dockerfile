FROM eclipse-temurin:17-jammy
RUN mkdir /opt/app
COPY application.yml /opt/app/application.yml
COPY target/webapp.jar /opt/app/webapp.jar
COPY target/fiji-bin-linux /opt/app/fiji-bin-linux
EXPOSE 8080
ENTRYPOINT [ "java", "-jar", "/opt/app/webapp.jar"]
CMD ["--spring.config.location", "/opt/app/application.yml"]