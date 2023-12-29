@echo off

for /f %%i in ('docker container ls ^| grep algatorweb ^| cut -f 1 -d" "') do set tID=%%i

IF DEFINED tID (
  echo ALGator docker container is already running. Container id: %tID%
  pause
) ELSE (
  docker run --mount type=bind,source=%ALGATOR_ROOT%/data_root,target=/home/algator/ALGATOR_ROOT/data_root -p 8081:8081 --env TPY=W algator/algatorweb
)