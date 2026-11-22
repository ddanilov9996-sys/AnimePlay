@echo off
chcp 65001 >nul
cd /d "%~dp0"
call gradlew.bat assembleDebug
if errorlevel 1 (
  echo.
  echo Сборка не удалась. Смотри текст ошибки выше.
  pause
  exit /b 1
)
echo.
echo Готово! APK находится в app\build\outputs\apk\debug\app-debug.apk
start "" "%~dp0app\build\outputs\apk\debug"
pause
