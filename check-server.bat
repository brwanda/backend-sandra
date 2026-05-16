@echo off
echo Checking EaraConnect Backend Server Status...
echo.

echo Testing connection to http://localhost:8080...
curl -s --connect-timeout 5 http://localhost:8080/actuator/health 2>nul
if %errorlevel% equ 0 (
    echo ✅ Server is running and healthy!
    echo.
    echo Testing SubCommittee API endpoint...
    curl -s http://localhost:8080/api/sub-committees
    echo.
    echo.
    echo ✅ SubCommittee API is accessible!
) else (
    echo ❌ Server is not running or not accessible
    echo.
    echo To start the server, run:
    echo   start-server.bat
    echo.
    echo Or manually:
    echo   mvn spring-boot:run
)

echo.
echo Checking if port 8080 is in use...
netstat -an | findstr :8080
if %errorlevel% equ 0 (
    echo ✅ Port 8080 is in use
) else (
    echo ❌ Port 8080 is not in use
)

pause
