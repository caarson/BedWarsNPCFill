
@echo off
if not exist paper.jar (
    echo Downloading PaperMC...
    powershell -Command "Invoke-WebRequest -Uri 'https://cdn.papermc.io/artifacts/paper/1.20.4/latest/paper-1.20.4-latest.jar' -OutFile 'paper.jar'"
)
echo Starting server...
java -jar paper.jar
pause
