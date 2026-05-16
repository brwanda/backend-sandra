@echo off
echo Starting EaraConnect Backend Server...
echo.

echo Compiling application...
call mvn compile
if %errorlevel% neq 0 (
    echo.
    echo ❌ Compilation failed! Please fix the errors above.
    pause
    exit /b 1
)

echo.
echo ✅ Compilation successful!
echo.
echo Starting Spring Boot server on http://localhost:8080
echo Press Ctrl+C to stop the server
echo.

call mvn spring-boot:run
