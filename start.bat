@echo off
REM Chatwolf Messaging App Startup Script

setlocal enabledelayedexpansion

REM Define colors and styling
color 0A
title Chatwolf Messaging App Startup

echo.
echo ==============================================================
echo   Starting Eureka Server...
echo ==============================================================

set EUREKA_SERVICE_PATH=D:\chatwolf2\eureka
set EUREKA_PORT=8761
set EUREKA_HEALTH_URL=http://localhost:%EUREKA_PORT%/actuator/health

cd /d %EUREKA_SERVICE_PATH%

REM Check if gradlew.bat exists
if not exist gradlew.bat (
    echo Error: gradlew.bat not found in %EUREKA_SERVICE_PATH%
    pause
    exit /b 1
)

REM Start Eureka using Gradle wrapper in a new window
start "Eureka Server" cmd /k "gradlew bootRun"

REM Wait for Eureka to be healthy
echo Waiting for Eureka server to start...
call :waitForService "%EUREKA_HEALTH_URL%" "Eureka Server"

echo.
echo ==============================================================
echo   Starting Gateway Service...
echo ==============================================================

set GATEWAY_PATH=D:\chatwolf2\gateway
set GATEWAY_PORT=8080
set GATEWAY_HEALTH_URL=http://localhost:%GATEWAY_PORT%/actuator/health

cd /d %GATEWAY_PATH%

REM Check if gradlew.bat exists
if not exist gradlew.bat (
    echo Error: gradlew.bat not found in %GATEWAY_PATH%
    pause
    exit /b 1
)

REM Start Gateway using Gradle wrapper in a new window
start "Gateway Service" cmd /k "gradlew bootRun"

REM Wait for Gateway to be healthy
echo Waiting for Gateway service to start...
call :waitForService "%GATEWAY_HEALTH_URL%" "Gateway"


REM Start Service 2
REM if exist %SERVICE2_PATH%\build.gradle (
REM     echo Starting Service 2 on port %SERVICE2_PORT%...
REM     cd /d %SERVICE2_PATH%
REM     start "Service 2" cmd /k "gradle bootRun"
REM     timeout /t 8 /nobreak
REM ) else (
REM     echo Warning: Service 2 build.gradle not found at %SERVICE2_PATH%
REM )

REM Start Service 3
REM if exist %SERVICE3_PATH%\build.gradle (
REM    echo Starting Service 3 on port %SERVICE3_PORT%...
REM    cd /d %SERVICE3_PATH%
REM    start "Service 3" cmd /k "gradle bootRun"
REM    timeout /t 8 /nobreak
REM ) else (
REM    echo Warning: Service 3 build.gradle not found at %SERVICE3_PATH%
REM )

echo.
echo ========================================
echo Startup Complete!
echo ========================================
echo.
echo eureka: %EUREKA_URL%
echo gateway: http://localhost:%GATEWAY_PORT%
REM echo Service 2 URL: http://localhost:%SERVICE2_PORT%
REM echo Service 3 URL: http://localhost:%SERVICE3_PORT%
echo.
echo All services are starting in separate windows
echo Close any window to stop that service
echo.
pause

echo.
echo ========================================
echo  Opening Eureka Server Dashboard...
echo ========================================
echo.

set EUREKA_URL=http://localhost:%EUREKA_PORT%

start "" "%EUREKA_URL%"

echo Eureka Dashboard opened at: %EUREKA_URL%
echo.

exit /b 0

REM ============================================
REM Function: Wait for Service to Start
REM ============================================
:waitForService
setlocal
set HEALTH_URL=%~1
set SERVICE_NAME=%~2
set MAX_ATTEMPTS=120
set ATTEMPT=0

:checkHealth
set /a ATTEMPT+=1

REM Use curl or powershell to check health endpoint
powershell -Command "try { $response = Invoke-WebRequest -Uri '%HEALTH_URL%' -UseBasicParsing -ErrorAction Stop; if ($response.StatusCode -eq 200) { exit 0 } } catch { exit 1 }"

if %errorlevel% equ 0 (
	echo .
    echo [SUCCESS] %SERVICE_NAME% is healthy and running!
    endlocal
    exit /b 0
)

if %ATTEMPT% geq %MAX_ATTEMPTS% (
    echo [TIMEOUT] %SERVICE_NAME% did not start within 120 seconds
    echo Check the service window for errors
    endlocal
    exit /b 1
)

REM Show progress
if %ATTEMPT% equ 1 (
    echo Checking health endpoint...
)

REM Wait 5 second before retrying
timeout /t 5 /nobreak > nul

goto checkHealth
