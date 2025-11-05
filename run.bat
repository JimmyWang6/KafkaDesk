@echo off
REM KafkaDesk launcher script for Windows

echo Starting KafkaDesk...

REM Check if Java is installed
where java >nul 2>nul
if %errorlevel% neq 0 (
    echo Error: Java is not installed or not in PATH
    echo Please install Java 11 or higher
    pause
    exit /b 1
)

REM Build if necessary
if not exist "target\kafkadesk-1.0.0.jar" (
    echo Building KafkaDesk...
    call mvn clean package
    if %errorlevel% neq 0 (
        echo Build failed!
        pause
        exit /b 1
    )
)

REM Run the application
java -jar target\kafkadesk-1.0.0.jar

exit /b 0
