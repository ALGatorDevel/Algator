###Installing ALGator on Linux like systems

1. **Install Docker**
   ALGator runs in a Docker container, so you need to install Docker on your system. For details see: https://docs.docker.com/get-docker/
<br>

2. **Create ALGator folder**
    Create a folder (for example, `/home/user_name/ALGATOR_ROOT`) that will be used to store ALGator configuration and other files. In the following we will refere to this folder as the `<algator_root>` folder.
    <br>

3. **Set environment variable $ALGATOR_ROOT** 
  
   Add the following line to `~/.bash_profile`:  
   <br>
	 ```
	   export ALGATOR_ROOT=<algator_root>	
	 ```
   Close and reopen the terminal to set the value to `$ALGATOR_ROOT`.
   <br>

4. **Download and run the instalation script** 
    Download the instalation script from GitHub ...
    <br>
    ```
    curl -L -O https://raw.github.com/ALGatorDevel/Algator/master/dist/bin/linux/algator_start
    ```   
    ... and run

    ```
    chmod +x algator_start
    ./algator_start
    ```
    This command will download and run the Docker image and perform initialization of ALGator system. To see the whole instalation procedure, click <a href="/dist/htmldoc/images/linux_install.png">here</a>.
    <br>

5. **Test the correctness of the instalation and run ALGator**
After the initialization, you can run ALGator using the scripts located in `$ALGATOR_ROOT/data_root/bin` folder.
<br>

    ``` 
    cd $ALGATOR_ROOT/data_root/bin
    ./algator_version
    ```


   Note: in the ALGator.zip there are two ready-to-use example projects: BasicSort 
   and BasicMatrixMul (in the `data_root` folder). Use these two project as a reference 
   and as an example-driven tutorial for your projects. 


