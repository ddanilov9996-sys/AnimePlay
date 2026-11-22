@echo off
setlocal
set "GRADLE_VERSION=9.6.0"
set "GRADLE_HOME=%USERPROFILE%\.gradle\animeplay\gradle-%GRADLE_VERSION%"
set "GRADLE_ZIP=%TEMP%\animeplay-gradle-%GRADLE_VERSION%-bin.zip"
set "GRADLE_URL=https://services.gradle.org/distributions/gradle-%GRADLE_VERSION%-bin.zip"
set "GRADLE_SHA256=bbaeb2fef8710818cf0e261201dab964c572f92b942812df0c3620d62a529a01"

if not exist "%GRADLE_HOME%\bin\gradle.bat" (
  echo [AnimePlay] Downloading official Gradle %GRADLE_VERSION%...
  powershell -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference='Stop'; $ProgressPreference='SilentlyContinue'; Invoke-WebRequest -Uri '%GRADLE_URL%' -OutFile '%GRADLE_ZIP%'; $actual=(Get-FileHash -Path '%GRADLE_ZIP%' -Algorithm SHA256).Hash.ToLower(); if ($actual -ne '%GRADLE_SHA256%') { throw 'Gradle SHA-256 verification failed.' }; $dest=Join-Path $env:USERPROFILE '.gradle\animeplay'; New-Item -ItemType Directory -Force -Path $dest | Out-Null; Expand-Archive -Path '%GRADLE_ZIP%' -DestinationPath $dest -Force; Remove-Item '%GRADLE_ZIP%' -Force"
  if errorlevel 1 exit /b 1
)

call "%GRADLE_HOME%\bin\gradle.bat" %*
exit /b %ERRORLEVEL%
