###Installing ALGator on Linux like systems

1. **Create ALGator folder**

    Create a folder (for example, `/home/user_name/ALGATOR_ROOT`) that will be used to store ALGator configuration and other files. In the following we will refere to this folder as the `<algator_root>` folder.
<br>

2. **Download algator_linux.zip** file from GitHub

    ```
    curl -L -O https://raw.github.com/ALGatorDevel/Algator/master/algator_linux.zip
    ```   

3. **Unpack the file the to <algator_root> folder**

	```
	$ cd <algator_root>
	$ unzip algator_linux.zip
	```

 
4. **Set environment variables** 
  
   Add the following lines to `~/.bash_profile`:
  
	```
	 export ALGATOR_ROOT=<algator_root>
	 export PATH=$PATH:$ALGATOR_ROOT/bin/
	```

5. **Test correctness of the instalation**

    ```java algator.Version```


   Note: in the ALGator.zip there are two ready-to-use example projects: BasicSort 
   and BasicMatrixMul (in the `data_root` folder). Use these two project as a reference 
   and as an example-driven tutorial for your projects. 


