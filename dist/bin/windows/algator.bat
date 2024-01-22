@echo off

setlocal enabledelayedexpansion

rem Set of valid actions
set "valid_values=start stop status execute admin analyse bash version webpage"

rem ***********************************************
rem Check if the first argument is in the set
rem ***********************************************
set "valid=false"
for %%v in (%valid_values%) do (
    if /i "%1" equ "%%v" (
        set "valid=true"
        goto :found
    )
)
:found
if not %valid%==true (
    echo Usage: algator command ^<args^>
    echo Valid commands: %valid_values%
    exit /b 1
)

rem one argument is defined, store it in cmd
rem   and  shift the array of arguments
set "cmd=%1"
shift
set "args="
:parse
if "%~1" neq "" (
  set args=%args% %1
  shift
  goto :parse
)
if defined args set args=%args:~1%
rem ***********************************************


rem ***********************************************
rem Gets the id of algator docker container
rem ***********************************************
set "containerName=algatorweb"
set "tID="

for /f "tokens=*" %%i in ('docker container ls 2^>^&1') do (
    set "line=%%i"    
    if not "!line:~0,5!"=="error" (
      for /f "tokens=2" %%j in ('echo " !line!" ^| find "algatorweb"') do (
        set "tID=%%j"
      )
    ) else (
      echo Docker is not running. Start docker and then try again.
      exit /b 1
    )  
)
if /I not "!cmd!"=="start" (
  if not defined tID (
    echo ALGator docker container is not running. Run "algator start" before using other commands.
    exit /b 1
  )
)
rem ***********************************************


rem Switch-like operation based on the first argument

if /i "!cmd!" equ "start" (

    IF DEFINED tID (
      echo ALGator docker container is already running. Container id: %tID%  
    ) ELSE (
      docker run --mount type=bind,source=%ALGATOR_ROOT%/data_root,target=/home/algator/ALGATOR_ROOT/data_root -p 3306:3306 -p 12321:12321 -p 8081:8081 --env TPY=W --env MYSQL_ROOT_HOST=% algator/algatorweb 
    )

) else if /i "!cmd!" equ "stop" (

  docker container kill %tID% 

) else if /i "!cmd!" equ "status" (

    IF DEFINED tID (
      echo ALGator docker container is running, container id: %tID%  
    ) ELSE (
      echo ALGator docker container is not running.
    )

) else if /i "!cmd!" equ "execute" (

    if not defined args (
      set /p args="Enter params for java algator.Execute: "
    ) 
    docker exec -i -t %tID% java algator.Execute !args!

) else if /i "!cmd!" equ "admin" (

    if not defined args (
      set /p args="Enter params for java algator.Admin: "
    ) 
    docker exec -i -t %tID% java algator.Admin !args!

) else if /i "!cmd!" equ "analyse" (

    if not defined args (
      set /p args="Enter params for java algator.Analyse: "
    ) 
    docker exec -i -t %tID% java algator.Analyse !args!

) else if /i "!cmd!" equ "bash" (

    docker exec -i -t %tID% /bin/bash

) else if /i "!cmd!" equ "version" (

    docker exec -i -t %tID% java algator.Version

) else if /i "!cmd!" equ "webpage" (

  start "" http://localhost:8081
  
) else (
    rem This should not be reached, as we already checked for valid values
    echo Internal Error: Unexpected argument.
    exit /b 1
)

pause