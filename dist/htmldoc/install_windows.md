## Installing ALGator on Windows

1. **Install Docker**
   ALGator runs in a Docker container, so you need to install Docker on your system. For details see: https://docs.docker.com/get-docker/
<br>

2. **Create ALGator folder**
    Create a folder (for example, `D:\ALGATOR_ROOT`) that will be used to store ALGator configuration and projects related files. In the following we will refere to this folder as the `<algator_root>` folder.
    <br>

3. **Set the %ALGATOR_ROOT% environment variable** 
  
   In Environment Variables (Start -> Edit the system environment variables) add the variable or type into command prompt:
   <br>
   ```
    setx ALGATOR_ROOT "<algator_root>"
    ```
   <br>
4. **Create data_root folder**
    Create a subfolder in %ALGATOR_ROOT%; this folder will be mounted into the docker container and therefore available to the ALGator tools.
    <br>
	 ```
	   C:\> cd /d %ALGATOR_ROOT%
       D:\ALGATOR_ROOT> md data_root
	 ```

5. **Download and execute the instalation script** 
    Download the instalation script from GitHub ...
    <br>
    ```
    D:\ALGATOR_ROOT> curl -L -O https://raw.github.com/ALGatorDevel/Algator/master/dist/bin/windows/ALGator_start.bat
    ```   
    ... and execute
    <br>
    ```
    D:\ALGATOR_ROOT> ALGator_start.bat
    ```
    This script will download and execute the Docker image and perform initialization of ALGator system. 
    <!--p align=right><a href="/dist/htmldoc/images/linux_install.png">Screenshot</a-->
    <br>

6. **Test the correctness of the instalation**
    After the initialization, you can use the ALGator with the scripts that are located in `%ALGATOR_ROOT%\data_root\bin` folder. But first, add this folder to the PATH ...
<br>
    ``` 
    C:\> setx PATH %PATH%;%ALGATOR_ROOT%\data_root\bin
    ```

    ... and check the version of the ALGator with 
<br>
    ``` 
    C:\> ALGator_version.bat
    ```
<br>

