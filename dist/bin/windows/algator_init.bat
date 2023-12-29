@echo off
docker run --mount type=bind,source=%ALGATOR_ROOT%/data_root,target=/home/algator/ALGATOR_ROOT/data_root -p 8081:8081 --env TPY=W algator/algatorweb
