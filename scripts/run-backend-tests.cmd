@echo off
setlocal
cd /d "%~dp0\.."
echo Running all SWT301 backend automated tests...
call mvnw.cmd clean test
if errorlevel 1 (
  echo Backend tests failed.
  exit /b 1
)
echo.
echo All backend automated tests passed.
echo Surefire reports: %CD%\target\surefire-reports
echo Coverage report:  %CD%\target\site\jacoco\index.html
