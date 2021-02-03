@echo off
for /f %%i in ('docker container ls ^| grep algatorweb ^| cut -f 1 -d" "') do set tID=%%i

IF NOT DEFINED tID (
  echo ALGator docker container is not running. 
  pause
) ELSE (
  docker container kill %tID%
)