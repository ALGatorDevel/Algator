## Installing ALGator on Linux like systems

1. **Install Docker**

   ALGator runs in a Docker container, so you need to install Docker on your system. For details see: https://docs.docker.com/get-docker/
<br>

2. **Create ALGator folder**

    Create a folder (for example, `/home/user_name/ALGATOR_ROOT`) that will be used to store ALGator configuration and projects related files. In the following we will refere to this folder as the `<algator_root>` folder.
    <br>

3. **Set environment variable $ALGATOR_ROOT** 
  
   Add the following line to `~/.bash_profile`:  
   <br>
	 ```
	   export ALGATOR_ROOT=<algator_root>	
	 ```
   Close and reopen the terminal (or call `source ~/.bashrc`) to apply changes.
   <br>
4. **Create data_root folder**
    
    Create a subfolder in $ALGATOR_ROOT; this folder will be mounted into the docker container and therefore available to the ALGator tools.
    <br>
	 ```
	   $ cd $ALGATOR_ROOT
       $ mkdir data_root
	 ```

5. **Download and execute the instalation script** 
    
    Download the instalation script from GitHub ...
    <br>
    ```
    $ curl -L -O https://raw.github.com/ALGatorDevel/Algator/master/dist/bin/_target_/algator_start
    ```   
    ... (note: replace <b>_target_</b>b> with <b>linux</b> or <b>mac</b>) and execute
    <br>
    ```
    $ chmod +x algator_start
    $ ./algator_start
    ```
    This script will download and execute the Docker image and perform initialization of ALGator system. 
    <p align=right><a href="/dist/htmldoc/images/linux_install.png">Screenshot</a>
    <br>

6. **Test the correctness of the instalation**
    
    After the initialization, you can use the ALGator with the scripts that are located in `$ALGATOR_ROOT/data_root/bin` folder. But first, add this folder to the PATH ...
    <br>
    ``` 
    $ export PATH=$PATH:$ALGATOR_ROOT/data_root/bin
    ```
    ... and check the version of the ALGator with 
    <br>
    ``` 
    $ algator_version
    ```
    <p align=right><a href="/dist/htmldoc/images/version.png">Screenshot</a>
    <br>

