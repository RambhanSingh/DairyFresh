@echo off
cd /d "%~dp0"
echo ============================================================
echo  Milk Distribution
echo  When you see "Started MilkApplication", open:
echo      http://localhost:8080
echo  Press Ctrl+C in this window to stop the app.
echo ============================================================
call mvn spring-boot:run
pause
