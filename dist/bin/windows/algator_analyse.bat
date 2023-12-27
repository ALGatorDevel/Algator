@echo off
for /f %%i in ('docker container ls ^| grep algatorweb ^| cut -f 1 -d" "') do set tID=%%i

if "%~1"=="" (
  set /p pars="Enter params for java algator.Analyse: "
) else (
  set pars=%*
)

IF NOT DEFINED tID (
  echo ALGator docker container is not running. Run algator_start before using other scripts.
  pause
) ELSE (
  set pars="Enter params for java algator.Analyse: "
  echo Analyse %pars%
  docker exec -i -t %tID% java algator.Analyse %pars%
  pause
)