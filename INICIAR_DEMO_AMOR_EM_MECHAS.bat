@echo off
setlocal
powershell.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\start-demo.ps1"
if errorlevel 1 (
    echo.
    echo A demonstracao nao foi iniciada.
)
echo.
pause
endlocal
