@echo off
for /f %%i in ('docker container ls ^| grep algatorweb ^| cut -f 1 -d" "') do set tID=%%i

IF NOT DEFINED tID (
  echo ALGator docker container is not running. Run ALGator_Start before using other scripts.
  pause
) ELSE (
  docker exec -i -t %tID% /bin/bash
)