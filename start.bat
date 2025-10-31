@echo off
REM Chatwolf Messaging App Startup Script - Enhanced Version
setlocal enabledelayedexpansion
color 0A
title Chatwolf Messaging App Startup

REM ==============================================================
REM   Configuration
REM ==============================================================
set "LOG_DIR=%~dp0logs"
set "LOG_FILE=%LOG_DIR%\startup_%date:~-4,4%%date:~-10,2%%date:~-7,2%_%time:~0,2%%time:~3,2%%time:~6,2%.log"
set "LOG_FILE=%LOG_FILE: =0%"

set "EUREKA_SERVICE_PATH=D:\chatwolf2\eureka"
set "EUREKA_PORT=8761"
set "EUREKA_HEALTH_URL=http://localhost:%EUREKA_PORT%/actuator/health"

set "GATEWAY_PATH=D:\chatwolf2\gateway"
set "GATEWAY_PORT=8080"
set "GATEWAY_HEALTH_URL=http://localhost:%GATEWAY_PORT%/actuator/health"

set "REDIS_PORT=6379"
set "REDIS_CONTAINER_NAME=redis-db"

set "MAX_STARTUP_ATTEMPTS=3"
set "SERVICE_TIMEOUT=180"

REM ==============================================================
REM   Initialize Logging
REM ==============================================================
if not exist "%LOG_DIR%" mkdir "%LOG_DIR%"
call :log "=========================================="
call :log "Chatwolf Startup Script - Enhanced"
call :log "Started at: %date% %time%"
call :log "=========================================="

echo.
echo ==============================================================
echo   Chatwolf Messaging App - Startup
echo ==============================================================
echo.
echo Log file: %LOG_FILE%
echo.

REM ==============================================================
REM   Pre-flight Checks
REM ==============================================================
call :log "Running pre-flight checks..."

REM Check if running as admin (optional but recommended for Docker)
net session >nul 2>&1
if %errorlevel% neq 0 (
    call :log "WARNING: Not running as administrator. Some operations may fail."
    echo [WARNING] Not running as administrator. Consider running as admin.
)

REM Check if Docker is installed and running
call :log "Checking Docker availability..."
docker --version >nul 2>&1
if %errorlevel% neq 0 (
    call :log "ERROR: Docker is not installed or not in PATH"
    echo [ERROR] Docker is not installed or not accessible.
    echo Please install Docker Desktop and ensure it's running.
    pause
    exit /b 1
)

docker info >nul 2>&1
if %errorlevel% neq 0 (
    call :log "ERROR: Docker daemon is not running"
    echo [ERROR] Docker daemon is not running.
    echo Please start Docker Desktop and try again.
    pause
    exit /b 1
)
call :log "Docker is available and running"

REM Check if Java is available
call :log "Checking Java availability..."
java -version >nul 2>&1
if %errorlevel% neq 0 (
    call :log "WARNING: Java not found in PATH"
    echo [WARNING] Java not found in PATH. Services may fail to start.
)

REM Verify service directories exist
if not exist "%EUREKA_SERVICE_PATH%" (
    call :log "ERROR: Eureka path not found: %EUREKA_SERVICE_PATH%"
    echo [ERROR] Eureka service directory not found: %EUREKA_SERVICE_PATH%
    pause
    exit /b 1
)

if not exist "%GATEWAY_PATH%" (
    call :log "ERROR: Gateway path not found: %GATEWAY_PATH%"
    echo [ERROR] Gateway service directory not found: %GATEWAY_PATH%
    pause
    exit /b 1
)

call :log "Pre-flight checks completed successfully"

REM ==============================================================
REM   Start Eureka Server
REM ==============================================================
echo.
echo ==============================================================
echo   Starting Eureka Server...
echo ==============================================================

call :log "Checking Eureka Server status..."
call :checkServiceHealth "%EUREKA_HEALTH_URL%" "Eureka Server"
if %ERRORLEVEL% equ 0 (
    call :log "Eureka Server is already running on port %EUREKA_PORT%"
    echo [INFO] Eureka Server is already running on port %EUREKA_PORT%
) else (
    call :log "Starting Eureka Server..."
    call :startJavaService "%EUREKA_SERVICE_PATH%" "Eureka Server" "%EUREKA_HEALTH_URL%" %SERVICE_TIMEOUT%
    if !ERRORLEVEL! neq 0 (
        call :log "ERROR: Failed to start Eureka Server"
        echo [ERROR] Failed to start Eureka Server. Check the log for details.
        pause
        exit /b 1
    )
)

REM ==============================================================
REM   Start Redis Container
REM ==============================================================
echo.
echo ==============================================================
echo   Starting Redis Container...
echo ==============================================================

call :log "Managing Redis container..."
call :startRedisContainer
if %ERRORLEVEL% neq 0 (
    call :log "ERROR: Failed to start Redis container"
    echo [ERROR] Failed to start Redis container. Check the log for details.
    pause
    exit /b 1
)

REM Wait for Redis to be ready with enhanced checks
call :log "Waiting for Redis to be ready..."
echo Waiting for Redis to be ready...
call :waitForRedis
if %ERRORLEVEL% neq 0 (
    call :log "ERROR: Redis failed to become ready within timeout period"
    echo [ERROR] Redis failed to become ready. Check the log for details.
    echo.
    echo You can check Redis logs with: docker logs %REDIS_CONTAINER_NAME%
    pause
    exit /b 1
)

echo [SUCCESS] Redis is ready and accepting connections.

REM ==============================================================
REM   Start Gateway Service
REM ==============================================================
echo.
echo ==============================================================
echo   Starting Gateway Service...
echo ==============================================================

call :log "Checking Gateway Service status..."
call :checkServiceHealth "%GATEWAY_HEALTH_URL%" "Gateway Service"
if %ERRORLEVEL% equ 0 (
    call :log "Gateway Service is already running on port %GATEWAY_PORT%"
    echo [INFO] Gateway Service is already running on port %GATEWAY_PORT%
) else (
    call :log "Starting Gateway Service..."
    call :startJavaService "%GATEWAY_PATH%" "Gateway Service" "%GATEWAY_HEALTH_URL%" %SERVICE_TIMEOUT%
    if !ERRORLEVEL! neq 0 (
        call :log "ERROR: Failed to start Gateway Service"
        echo [ERROR] Failed to start Gateway Service. Check the log for details.
        pause
        exit /b 1
    )
)

REM ==============================================================
REM   Startup Complete
REM ==============================================================
echo.
echo ==============================================================
echo   Startup Complete!
echo ==============================================================

call :log "All services started successfully"

echo.
echo Services Running:
echo   Eureka Server:  http://localhost:%EUREKA_PORT%
echo   Gateway:        http://localhost:%GATEWAY_PORT%
echo   Redis:          localhost:%REDIS_PORT%
echo.
echo All services started in separate windows.
echo Close any window to stop that service.
echo.
echo Log file: %LOG_FILE%
echo.

pause
call :log "Startup script completed"
exit /b 0

REM ==============================================================
REM   Function: Log Message
REM ==============================================================
:log
echo [%date% %time%] %~1 >> "%LOG_FILE%"
exit /b 0

REM ==============================================================
REM   Function: Check Service Health
REM ==============================================================
:checkServiceHealth
setlocal
set "HEALTH_URL=%~1"
set "SERVICE_NAME=%~2"

powershell -Command "try { $response = Invoke-WebRequest -Uri '%HEALTH_URL%' -UseBasicParsing -TimeoutSec 3 -ErrorAction Stop; if ($response.StatusCode -eq 200) { exit 0 } else { exit 1 } } catch { exit 1 }" >nul 2>&1
endlocal & exit /b %errorlevel%

REM ==============================================================
REM   Function: Start Java Service
REM ==============================================================
:startJavaService
setlocal
set "SERVICE_PATH=%~1"
set "SERVICE_NAME=%~2"
set "HEALTH_URL=%~3"
set "TIMEOUT=%~4"

cd /d "%SERVICE_PATH%" 2>nul
if %errorlevel% neq 0 (
    call :log "ERROR: Cannot access directory: %SERVICE_PATH%"
    endlocal & exit /b 1
)

if not exist "gradlew.bat" (
    call :log "ERROR: gradlew.bat not found in %SERVICE_PATH%"
    endlocal & exit /b 1
)

call :log "Launching %SERVICE_NAME%..."
echo Starting %SERVICE_NAME%...

REM Start the service in a new window
REM start "%SERVICE_NAME%" cmd /k "echo Starting %SERVICE_NAME%... && gradlew.bat bootRun || (echo [ERROR] Failed to start && pause)"

REM Start service in background without new window
start /B "" gradlew.bat bootRun > "%SERVICE_NAME%.log" 2>&1

REM Wait for service to become healthy
call :log "Waiting for %SERVICE_NAME% to become healthy (timeout: %TIMEOUT%s)..."
echo Waiting for %SERVICE_NAME% to start...

set ATTEMPT=0
set /a MAX_ATTEMPTS=%TIMEOUT%/5

:waitLoop
set /a ATTEMPT+=1
timeout /t 5 /nobreak >nul

powershell -Command "try { $response = Invoke-WebRequest -Uri '%HEALTH_URL%' -UseBasicParsing -TimeoutSec 3 -ErrorAction Stop; if ($response.StatusCode -eq 200) { exit 0 } else { exit 1 } } catch { exit 1 }" >nul 2>&1
if %errorlevel% equ 0 (
    call :log "SUCCESS: %SERVICE_NAME% is healthy and running!"
    echo [SUCCESS] %SERVICE_NAME% is healthy and running!
    endlocal & exit /b 0
)

if %ATTEMPT% geq %MAX_ATTEMPTS% (
    call :log "TIMEOUT: %SERVICE_NAME% did not start within %TIMEOUT% seconds"
    echo [TIMEOUT] %SERVICE_NAME% did not start within %TIMEOUT% seconds.
    echo Check the service window for error messages.
    endlocal & exit /b 1
)

if %ATTEMPT% equ 1 echo Checking health endpoint...
if %ATTEMPT% equ 6 echo Still waiting... (%ATTEMPT%/%MAX_ATTEMPTS% checks)
if %ATTEMPT% equ 12 echo Service is taking longer than expected... (%ATTEMPT%/%MAX_ATTEMPTS% checks)
goto waitLoop

REM ==============================================================
REM   Function: Start Redis Container
REM ==============================================================
:startRedisContainer
setlocal

REM Check if container is running
call :log "Checking if Redis container is running..."
for /f "delims=" %%x in ('docker ps --filter "name=^%REDIS_CONTAINER_NAME%$" --filter "status=running" --format "{{.Names}}" 2^>nul') do (
    set "RUNNING=%%x"
)

if defined RUNNING (
    call :log "Redis container is already running"
    echo [INFO] Redis container '%REDIS_CONTAINER_NAME%' is already running.
    endlocal & exit /b 0
)

REM Check if container exists but is stopped
call :log "Checking if Redis container exists..."
for /f "delims=" %%x in ('docker ps -a --filter "name=^%REDIS_CONTAINER_NAME%$" --format "{{.Names}}" 2^>nul') do (
    set "EXISTS=%%x"
)

if defined EXISTS (
    call :log "Starting existing Redis container..."
    echo Starting existing Redis container...
    docker start %REDIS_CONTAINER_NAME% >nul 2>&1
    if !errorlevel! equ 0 (
        call :log "SUCCESS: Redis container started"
        echo [SUCCESS] Redis container started successfully.
        endlocal & exit /b 0
    ) else (
        call :log "ERROR: Failed to start existing Redis container"
        echo [ERROR] Failed to start existing Redis container.
        echo Attempting to remove and recreate...
        docker rm -f %REDIS_CONTAINER_NAME% >nul 2>&1
    )
)

REM Create new container
call :log "Creating new Redis container..."
echo Creating new Redis container...
docker run -d --name %REDIS_CONTAINER_NAME% -p %REDIS_PORT%:6379 --restart unless-stopped redis:8.2.2-alpine >nul 2>&1
if %errorlevel% equ 0 (
    call :log "SUCCESS: Redis container created and started"
    echo [SUCCESS] New Redis container created and started.
    endlocal & exit /b 0
) else (
    call :log "ERROR: Failed to create Redis container"
    echo [ERROR] Failed to create new Redis container.
    endlocal & exit /b 1
)

REM ==============================================================
REM   Function: Wait for Redis
REM ==============================================================
:waitForRedis
setlocal
set ATTEMPT=0
set MAX_ATTEMPTS=12

:redisCheck
set /a ATTEMPT+=1
docker exec %REDIS_CONTAINER_NAME% redis-cli ping >nul 2>&1
if %errorlevel% equ 0 (
    call :log "Redis is responding to PING"
    echo [SUCCESS] Redis is ready.
    endlocal & exit /b 0
)

if %ATTEMPT% geq %MAX_ATTEMPTS% (
    call :log "WARNING: Redis health check timed out"
    echo [WARNING] Redis health check timed out.
    endlocal & exit /b 1
)

timeout /t 2 /nobreak >nul
goto redisCheck

REM ==============================================================
REM   Function: Wait for Redis
REM ==============================================================
:waitForRedis
setlocal
set ATTEMPT=0
set MAX_ATTEMPTS=30
set WAIT_SECONDS=2

call :log "Starting Redis health checks (max %MAX_ATTEMPTS% attempts, %WAIT_SECONDS%s interval)"

:redisCheck
set /a ATTEMPT+=1

REM Method 1: Check if container is running
call :log "Attempt %ATTEMPT%/%MAX_ATTEMPTS%: Checking if container is running..."
docker ps --filter "name=%REDIS_CONTAINER_NAME%" --format "{{.Names}}" 2>nul | findstr /x "%REDIS_CONTAINER_NAME%" >nul 2>&1
if %errorlevel% neq 0 (
    call :log "ERROR: Redis container stopped unexpectedly"
    echo [ERROR] Redis container is not running!
    endlocal & exit /b 1
)

REM Method 2: PING check
call :log "Checking Redis PING response..."
docker exec %REDIS_CONTAINER_NAME% redis-cli ping 2>nul | findstr /i "PONG" >nul 2>&1
if %errorlevel% equ 0 (
    call :log "Redis PING successful"
    
    REM Method 3: Test actual connection with INFO command
    call :log "Testing Redis INFO command..."
    docker exec %REDIS_CONTAINER_NAME% redis-cli INFO server 2>nul | findstr /i "redis_version" >nul 2>&1
    if !errorlevel! equ 0 (
        call :log "Redis INFO command successful"
        
        REM Method 4: Test SET/GET operation
        call :log "Testing Redis SET/GET operations..."
        docker exec %REDIS_CONTAINER_NAME% redis-cli SET health_check "test" EX 10 >nul 2>&1
        if !errorlevel! equ 0 (
            docker exec %REDIS_CONTAINER_NAME% redis-cli GET health_check 2>nul | findstr /i "test" >nul 2>&1
            if !errorlevel! equ 0 (
                call :log "Redis SET/GET test successful - Redis is fully ready"
                echo [SUCCESS] Redis is healthy and ready (verified with PING, INFO, and SET/GET)
                
                REM Cleanup test key
                docker exec %REDIS_CONTAINER_NAME% redis-cli DEL health_check >nul 2>&1
                
                endlocal & exit /b 0
            )
        )
    )
)

REM Show progress indicator
if %ATTEMPT% equ 1 echo Performing health checks...
if %ATTEMPT% equ 5 echo Still waiting for Redis to initialize... (%ATTEMPT%/%MAX_ATTEMPTS%)
if %ATTEMPT% equ 10 echo Redis is taking longer than expected... (%ATTEMPT%/%MAX_ATTEMPTS%)
if %ATTEMPT% equ 20 echo Almost at timeout... (%ATTEMPT%/%MAX_ATTEMPTS%)

REM Check if we've exceeded max attempts
if %ATTEMPT% geq %MAX_ATTEMPTS% (
    call :log "TIMEOUT: Redis did not become ready within %MAX_ATTEMPTS% attempts"
    echo [TIMEOUT] Redis did not become ready within %MAX_ATTEMPTS% attempts (%WAIT_SECONDS%s each).
    echo.
    echo Troubleshooting steps:
    echo   1. Check container logs: docker logs %REDIS_CONTAINER_NAME%
    echo   2. Check if port %REDIS_PORT% is available: netstat -ano ^| findstr ":%REDIS_PORT%"
    echo   3. Try restarting: docker restart %REDIS_CONTAINER_NAME%
    echo   4. Check Docker resources (CPU/Memory)
    endlocal & exit /b 1
)

REM Wait before next attempt
timeout /t %WAIT_SECONDS% /nobreak >nul
goto redisCheck
