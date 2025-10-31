@echo off
REM Chatwolf Messaging App Stop Script - Enhanced Version
setlocal enabledelayedexpansion
color 0C
title Chatwolf Messaging App Shutdown

REM ==============================================================
REM   Configuration
REM ==============================================================
set "LOG_DIR=%~dp0logs"
set "LOG_FILE=%LOG_DIR%\shutdown_%date:~-4,4%%date:~-10,2%%date:~-7,2%_%time:~0,2%%time:~3,2%%time:~6,2%.log"
set "LOG_FILE=%LOG_FILE: =0%"

set "EUREKA_PORT=8761"
set "GATEWAY_PORT=8080"
set "REDIS_PORT=6379"
set "REDIS_CONTAINER_NAME=redis-db"

set "SHUTDOWN_TIMEOUT=15"
set "FORCE_KILL_DELAY=10"

REM ==============================================================
REM   Initialize Logging
REM ==============================================================
if not exist "%LOG_DIR%" mkdir "%LOG_DIR%"
call :log "=========================================="
call :log "Chatwolf Shutdown Script - Enhanced"
call :log "Started at: %date% %time%"
call :log "=========================================="

echo.
echo ==============================================================
echo   Chatwolf Messaging App - Shutdown
echo ==============================================================
echo.
echo Log file: %LOG_FILE%
echo.
echo This will stop all Chatwolf services:
echo   - Gateway Service (port %GATEWAY_PORT%)
echo   - Eureka Server (port %EUREKA_PORT%)
echo   - Redis Container
echo.

set /p "CONFIRM=Are you sure you want to stop all services? (Y/N): "
if /i not "%CONFIRM%"=="Y" (
    call :log "Shutdown cancelled by user"
    echo Shutdown cancelled.
    pause
    exit /b 0
)

echo.
call :log "User confirmed shutdown"

REM ==============================================================
REM   Stop Gateway Service
REM ==============================================================
echo.
echo ==============================================================
echo   Stopping Gateway Service...
echo ==============================================================

call :log "Attempting to stop Gateway Service on port %GATEWAY_PORT%"
call :stopServiceByPort %GATEWAY_PORT% "Gateway Service"
if %ERRORLEVEL% equ 0 (
    echo [SUCCESS] Gateway Service stopped successfully.
) else (
    echo [INFO] Gateway Service was not running or already stopped.
)

REM ==============================================================
REM   Stop Eureka Server
REM ==============================================================
echo.
echo ==============================================================
echo   Stopping Eureka Server...
echo ==============================================================

call :log "Attempting to stop Eureka Server on port %EUREKA_PORT%"
call :stopServiceByPort %EUREKA_PORT% "Eureka Server"
if %ERRORLEVEL% equ 0 (
    echo [SUCCESS] Eureka Server stopped successfully.
) else (
    echo [INFO] Eureka Server was not running or already stopped.
)

REM ==============================================================
REM   Stop Redis Container
REM ==============================================================
echo.
echo ==============================================================
echo   Stopping Redis Container...
echo ==============================================================

call :log "Attempting to stop Redis container"
call :stopRedisContainer
if %ERRORLEVEL% equ 0 (
    echo [SUCCESS] Redis container stopped successfully.
) else (
    echo [INFO] Redis container was not running or already stopped.
)

REM ==============================================================
REM   Verify All Services Stopped
REM ==============================================================
echo.
echo ==============================================================
echo   Verifying Services Stopped...
echo ==============================================================

call :log "Verifying all services are stopped"
set "ALL_STOPPED=1"

REM Check Gateway
call :isPortInUse %GATEWAY_PORT%
if %ERRORLEVEL% equ 0 (
    call :log "WARNING: Gateway port %GATEWAY_PORT% still in use"
    echo [WARNING] Gateway Service may still be running on port %GATEWAY_PORT%
    set "ALL_STOPPED=0"
) else (
    echo [OK] Gateway Service stopped - port %GATEWAY_PORT% is free
)

REM Check Eureka
call :isPortInUse %EUREKA_PORT%
if %ERRORLEVEL% equ 0 (
    call :log "WARNING: Eureka port %EUREKA_PORT% still in use"
    echo [WARNING] Eureka Server may still be running on port %EUREKA_PORT%
    set "ALL_STOPPED=0"
) else (
    echo [OK] Eureka Server stopped - port %EUREKA_PORT% is free
)

REM Check Redis - Fixed detection
docker container ps -a --filter "name=%REDIS_CONTAINER_NAME%" --format "{{.Names}} {{.Status}}" 2>nul | findstr /i "Up" >nul
if %ERRORLEVEL% equ 0 (
    call :log "WARNING: Redis container still running"
    echo [WARNING] Redis container is still running
    set "ALL_STOPPED=0"
) else (
    echo [OK] Redis container stopped
)

REM ==============================================================
REM   Cleanup
REM ==============================================================
echo.
echo ==============================================================
echo   Cleanup
echo ==============================================================

call :log "Checking for orphaned Java processes..."
call :cleanupOrphanedProcesses

REM ==============================================================
REM   Shutdown Complete
REM ==============================================================
echo.
echo ==============================================================
echo   Shutdown Complete!
echo ==============================================================

if "%ALL_STOPPED%"=="1" (
    call :log "All services stopped successfully"
    echo.
    echo [SUCCESS] All services have been stopped successfully.
    echo.
) else (
    call :log "Some services may still be running"
    echo.
    echo [WARNING] Some services may still be running.
    echo Please check the processes manually if needed.
    echo.
    echo You can use these commands to force kill:
    echo   - Task Manager: Find and end "java.exe" processes
    echo   - Command: taskkill /F /IM java.exe
    echo   - Docker: docker stop %REDIS_CONTAINER_NAME%
    echo.
)

echo Log file: %LOG_FILE%
echo.

call :log "Shutdown script completed"
pause
exit /b 0

REM ==============================================================
REM   Function: Log Message
REM ==============================================================
:log
echo [%date% %time%] %~1 >> "%LOG_FILE%"
exit /b 0

REM ==============================================================
REM   Function: Stop Service by Port
REM ==============================================================
:stopServiceByPort
setlocal
set "PORT=%~1"
set "SERVICE_NAME=%~2"

call :log "Checking if port %PORT% is in use..."

REM Find process using the port
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":%PORT% " ^| findstr "LISTENING"') do (
    set "PID=%%a"
    goto :foundProcess
)

REM Port not in use
call :log "Port %PORT% is not in use"
endlocal & exit /b 1

:foundProcess
if not defined PID (
    call :log "No process found using port %PORT%"
    endlocal & exit /b 1
)

call :log "Found process PID %PID% using port %PORT%"
echo Found %SERVICE_NAME% running with PID %PID%

REM Get process name for confirmation
for /f "tokens=1" %%a in ('tasklist /FI "PID eq %PID%" /NH /FO CSV') do (
    set "PROCESS_NAME=%%~a"
)

echo Process: !PROCESS_NAME! (PID: %PID%)

REM Try graceful shutdown
echo Attempting graceful shutdown...
call :log "Attempting graceful shutdown of PID %PID%"
taskkill /PID %PID% >nul 2>&1

REM Wait longer for graceful shutdown
echo Waiting %SHUTDOWN_TIMEOUT% seconds for graceful shutdown...
timeout /t %SHUTDOWN_TIMEOUT% /nobreak >nul

REM Check if process stopped
tasklist /FI "PID eq %PID%" 2>nul | findstr /i "%PID%" >nul
if %ERRORLEVEL% neq 0 (
    call :log "Process %PID% stopped gracefully"
    echo [SUCCESS] Process stopped gracefully.
    endlocal & exit /b 0
)

REM If still running, force kill
call :log "Graceful shutdown failed, forcing termination of PID %PID%"
echo Graceful shutdown timed out. Forcing termination...
taskkill /F /PID %PID% >nul 2>&1

timeout /t 3 /nobreak >nul

REM Verify process is gone
tasklist /FI "PID eq %PID%" 2>nul | findstr /i "%PID%" >nul
if %ERRORLEVEL% neq 0 (
    call :log "Process %PID% terminated successfully"
    echo [SUCCESS] Process terminated successfully.
    endlocal & exit /b 0
) else (
    call :log "ERROR: Failed to terminate process %PID%"
    echo [ERROR] Failed to terminate process. Manual intervention required.
    endlocal & exit /b 1
)

REM ---- Summary ----
if !FOUND_COUNT! equ 0 (
    call :log "No CMD windows found for PID %TARGET_PID%"
    endlocal & exit /b 0
) else (
    call :log "Successfully processed !FOUND_COUNT! CMD window(s)"
    endlocal & exit /b 0
)

REM ==============================================================
REM   Function: Stop Redis Container
REM ==============================================================
:stopRedisContainer
setlocal

REM Check if Docker is available
docker --version >nul 2>&1
if %errorlevel% neq 0 (
    call :log "WARNING: Docker not available"
    echo [WARNING] Docker is not available. Cannot stop Redis container.
    endlocal & exit /b 1
)

REM Check if container exists
call :log "Checking if Redis container exists..."
docker container ps -a --filter "name=%REDIS_CONTAINER_NAME%" --format "{{.Names}}" >nul 2>nul
if %ERRORLEVEL% neq 0 (
    call :log "Redis container does not exist"
    echo [INFO] Redis container '%REDIS_CONTAINER_NAME%' does not exist.
    endlocal & exit /b 1
)

for /f "tokens=*" %%i in ('docker container ps --filter "name=%REDIS_CONTAINER_NAME%" --format "{{.Names}} {{.Status}}" 2^>nul') do (
    set "CONTAINER_STATUS=%%i"
)

if defined CONTAINER_STATUS (
	call :log "Redis container status: !CONTAINER_STATUS!"
	echo Container status: !CONTAINER_STATUS!
)

REM Check if it's running (status contains "Up")
echo !CONTAINER_STATUS! | findstr /i "Up" >nul
if %ERRORLEVEL% neq 0 (
    call :log "Redis container is not running"
    echo [INFO] Redis container is already stopped.
    endlocal & exit /b 1
)

call :log "Stopping Redis container: %REDIS_CONTAINER_NAME%"
echo Stopping Redis container...

docker stop %REDIS_CONTAINER_NAME% >nul 2>&1
if %ERRORLEVEL% equ 0 (
    call :log "Redis container stopped successfully"
    
    REM Verify it's actually stopped
    timeout /t 2 /nobreak >nul
    docker ps --filter "name=%REDIS_CONTAINER_NAME%" --format "{{.Names}}" 2>nul | findstr /x "%REDIS_CONTAINER_NAME%" >nul
    if %ERRORLEVEL% neq 0 (
        echo [SUCCESS] Redis container stopped and verified.
        endlocal & exit /b 0
    ) else (
        call :log "WARNING: Container stop command succeeded but container still shows as running"
        echo [WARNING] Container may still be stopping...
        endlocal & exit /b 0
    )
) else (
    call :log "ERROR: Failed to stop Redis container"
    echo [ERROR] Failed to stop Redis container.
    echo Attempting force stop...
    docker kill %REDIS_CONTAINER_NAME% >nul 2>&1
    if !errorlevel! equ 0 (
        call :log "Redis container force stopped"
        echo [SUCCESS] Container force stopped.
        endlocal & exit /b 0
    ) else (
        call :log "ERROR: Failed to force stop Redis container"
        echo [ERROR] Could not force stop container.
        endlocal & exit /b 1
    )
)

REM ==============================================================
REM   Function: Check if Port is in Use
REM ==============================================================
:isPortInUse
setlocal
set "PORT=%~1"

netstat -ano | findstr ":%PORT% " | findstr "LISTENING" >nul 2>&1
endlocal & exit /b %errorlevel%

REM ==============================================================
REM   Function: Cleanup Orphaned Processes
REM ==============================================================
:cleanupOrphanedProcesses
setlocal

REM Look for Java processes that might be running Gradle
call :log "Searching for orphaned Java/Gradle processes..."

set "FOUND_ORPHANS=0"

REM Check for java.exe processes with bootRun in command line
for /f "skip=1 tokens=2" %%a in ('wmic process where "name='java.exe' and commandline like '%%bootRun%%'" get processid 2^>nul') do (
    set "TEST_PID=%%a"
    if defined TEST_PID (
        REM Verify it's a valid PID (not empty line)
        echo !TEST_PID! | findstr /r "^[0-9][0-9]*$" >nul
        if !errorlevel! equ 0 (
            set "FOUND_ORPHANS=1"
            call :log "Found orphaned bootRun process: PID !TEST_PID!"
            echo Found orphaned Gradle bootRun process (PID !TEST_PID!)
        )
    )
)

if "%FOUND_ORPHANS%"=="1" (
    echo.
    set /p "CLEANUP=Kill orphaned processes? (Y/N): "
    if /i "!CLEANUP!"=="Y" (
        call :log "User confirmed cleanup of orphaned processes"
        for /f "skip=1 tokens=2" %%a in ('wmic process where "name='java.exe' and commandline like '%%bootRun%%'" get processid 2^>nul') do (
            set "TEST_PID=%%a"
            if defined TEST_PID (
                echo !TEST_PID! | findstr /r "^[0-9][0-9]*$" >nul
                if !errorlevel! equ 0 (
                    call :log "Killing orphaned process PID !TEST_PID!"
                    
                    REM Close associated CMD windows first
                    call :closeCmdWindows !TEST_PID!
                    
                    REM Then kill the process
                    taskkill /F /PID !TEST_PID! >nul 2>&1
                    echo Killed process PID !TEST_PID!
                )
            )
        )
        echo [SUCCESS] Orphaned processes cleaned up.
    ) else (
        call :log "User declined cleanup of orphaned processes"
        echo Orphaned processes left running.
    )
) else (
    echo No orphaned processes found.
)

endlocal & exit /b 0
