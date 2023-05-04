FROM eclipse-temurin:17-jdk-jammy

# Install xvfb
RUN apt update && DEBIAN_FRONTEND=noninteractive apt-get install -yq xvfb xorg

# Copy web app & Fiji
RUN mkdir /opt/app
COPY target/webapp.jar /opt/app/webapp.jar
COPY target/fiji-bin-linux /opt/app/fiji-bin-linux
ENV RUNTIME_FIJI_PATH=/opt/app/fiji-bin-linux
ENV RUNTIME_FIJI_EXECUTABLE_PATH=/opt/app/fiji-bin-linux/ImageJ-linux64

# Fiji memory configuration
ENV RUNTIME_FIJI_ARGS_0=--memory
ENV RUNTIME_FIJI_ARGS_1=8G

# Runtime number of workers
ENV ORG_JOBRUNR_BACKGROUND_JOB_SERVER_WORKER_COUNT=1

# Admin configuration
ENV ACCOUNTS_ADMIN_USERNAME=admin@docker
ENV ACCOUNTS_ADMIN_PASSWORD=DeIiZCTDuqRDZbdF

EXPOSE 8080
ENTRYPOINT [ "java", "-jar", "/opt/app/webapp.jar"]