@ECHO OFF
SETLOCAL

set WRAPPER_DIR=%~dp0.mvn\wrapper
set DISTRIBUTION_DIR=%WRAPPER_DIR%\apache-maven
set DISTRIBUTION_URL=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.9/apache-maven-3.9.9-bin.zip
set MAVEN_HOME=%DISTRIBUTION_DIR%\apache-maven-3.9.9
set ARCHIVE_PATH=%WRAPPER_DIR%\apache-maven-3.9.9-bin.zip

if not exist "%MAVEN_HOME%\bin\mvn.cmd" (
  powershell -NoProfile -ExecutionPolicy Bypass -Command ^
    "$ProgressPreference='SilentlyContinue';" ^
    "New-Item -ItemType Directory -Force -Path '%DISTRIBUTION_DIR%' | Out-Null;" ^
    "if (-not (Test-Path '%ARCHIVE_PATH%')) { Invoke-WebRequest -Uri '%DISTRIBUTION_URL%' -OutFile '%ARCHIVE_PATH%'; }" ^
    "Expand-Archive -Path '%ARCHIVE_PATH%' -DestinationPath '%DISTRIBUTION_DIR%' -Force;"
)

"%MAVEN_HOME%\bin\mvn.cmd" %*
