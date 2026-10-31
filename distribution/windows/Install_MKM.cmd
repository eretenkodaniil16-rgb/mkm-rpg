@echo off
setlocal
cd /d "%~dp0"
where powershell.exe >nul 2>nul
if errorlevel 1 (
  echo [MKM] Windows PowerShell is unavailable.
  echo Install MKM manually using the official Fabric installer: https://fabricmc.net/use/
  pause
  exit /b 1
)
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0Install_MKM.ps1" %*
set "MKM_RESULT=%ERRORLEVEL%"
if not "%MKM_RESULT%"=="0" (
  echo.
  echo [MKM] Installation did not complete. Read the error above.
) else (
  echo.
  echo [MKM] Installation finished. Open Minecraft Launcher and select Fabric 26.3.
)
echo.
pause
exit /b %MKM_RESULT%
