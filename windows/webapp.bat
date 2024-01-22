@echo off

set CURRENT_FOLDER=%~dp0

rem Set Fiji paths
set RUNTIME_FIJI_PATH=%CURRENT_FOLDER%\app\fiji-bin-windows
set RUNTIME_FIJI_EXECUTABLE_PATH=%CURRENT_FOLDER%\app\fiji-bin-windows\ImageJ-win64.exe

rem Fiji memory configuration
set RUNTIME_FIJI_ARGS_0=--memory
set RUNTIME_FIJI_ARGS_1=8G

rem Runtime number of workers
set ORG_JOBRUNR_BACKGROUND_JOB_SERVER_WORKER_COUNT=1

rem Admin configuration
set ACCOUNTS_ADMIN_USERNAME=admin@local
set ACCOUNTS_ADMIN_PASSWORD=admin

rem Persistence
set RUNTIME_CUSTOM_TEMP_DIRECTORY=%CURRENT_FOLDER%\storage
mkdir %RUNTIME_CUSTOM_TEMP_DIRECTORY%

echo ----------------------------------------------------------------
echo [i] The login details for the admin account are
echo User: %ACCOUNTS_ADMIN_USERNAME%
echo Password: %ACCOUNTS_ADMIN_PASSWORD%
echo ----------------------------------------------------------------
echo 

echo
echo Continuing in 5s ...
timeout /t 5 /nobreak >nul

start "Spring Boot Server" cmd /c %CURRENT_FOLDER%\app\jre\bin\java.exe -jar %CURRENT_FOLDER%\app\webapp.jar

echo Waiting 10s until the server is started ...
timeout /t 10 /nobreak >nul
start "" "http://localhost:%SERVER_PORT%"