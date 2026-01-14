@echo off
setlocal

set "MAVEN_VERSION=3.9.9"
set "MAVEN_HOME=%USERPROFILE%\.m2\wrapper\apache-maven-%MAVEN_VERSION%"
set "MAVEN_BIN=%MAVEN_HOME%\bin\mvn.cmd"

if exist "%MAVEN_BIN%" goto :runMaven

echo Téléchargement de Maven %MAVEN_VERSION%...
powershell -Command "& { $url = 'https://archive.apache.org/dist/maven/maven-3/%MAVEN_VERSION%/binaries/apache-maven-%MAVEN_VERSION%-bin.zip'; $output = '%TEMP%\maven.zip'; Invoke-WebRequest -Uri $url -OutFile $output; Expand-Archive -Path $output -DestinationPath '%USERPROFILE%\.m2\wrapper' -Force; Remove-Item $output }"

if not exist "%MAVEN_BIN%" (
    echo Erreur lors du téléchargement de Maven
    exit /b 1
)

echo Maven installé avec succès!

:runMaven
"%MAVEN_BIN%" %*
