<h1 align="center"><img src="doc/images/algator.png" alt="ALGator logo" /></h1>
<h4 align="center">An automatic algorithm evaluation system </h4>
<br>

## About the ALGator

ALGator facilitates automatic algorithm evaluation process by executing 
implementations of the algorithms on the given predefined sets of test cases
and analyzing various indicators of the execution.  
To use the ALGator, user defines a project including the definition of 
the problem to be solved, sets of test cases, parameters 
of the input and indicators of the output data  and the criteria for the 
algorithm quality evaluation. When a project is defined, any number of 
algorithm implementations can be added. When requested, system 
executes all the implemented algorithms, checks for the correctness 
and compares the quality of their results. Using the ALGator user can 
add additional quality criteria, draw graphs and perform evaluations and 
comparisons of defined algorithms. 

## Instalation 

A single-user version instalation of the ALGator on Windows system (see also a <a href="README.md">Linux version</a> of this document):


1. **Create ALGator folder**

    We will refere to this folder as `<algator_root>` folder

2. **Download ALGator.zip** file from GitHub

    ```
    curl -L -O https://raw.github.com/ALGatorDevel/Algator/master/ALGator.zip
    ```

3. **Unpack ALGator.zip** file to `<algator_root>`

    ```
	C:\> cd <algator_root>
	C:\algator_root> unzip ALGator.zip
	```


4. **Set environment variables** 

   In Environment Variables (Start -> Edit the system environment variables) add variables or type into command prompt:

   ```
    setx ALGATOR_ROOT "<algator_root>"
    setx CLASSPATH "%CLASSPATH%;%ALGATOR_ROOT%/app/ALGator/ALGator.jar"
   ```

   **Note**: in the first line change the `<algator_root>` with the name of 
   your ALGator folder.



5. **To test correctness** of the instalation, type

    ```java algator.Version``` 

    into a command prompt.



6. **Install rsync**
    - From https://cygwin.com/install.html install cygwin and rsync.

    - Add ```C:\cygwin64\bin\``` to PATH in system environment. To test it run ```rsync``` in command prompt.



## Usage examples

1. To **run** an existing algorithm on a selected testset

  ```java algator.Execute BasicSort -a BubbleSort -t TestSet1```

2. To **run** all tests of a project

  ```java algator.Execute BasicSort```
  
3. To **plot a chart** of the results

  ```java algator.Chart BasicSort```
  
4. To **create** a new project

  ```java algator.Admin -cp <project_name>```

  Note: when the project is created you have to edit the following files:
  `<project_name>Input.java`, `<project_name>Output.java`, `<project_name>TestCase.java` 
  and `<project_name>AbsAlgorithm.java` which are placed in the `src`folder of 
  the project.
  
5. To **create** a new algorithm 

  ```java algator.Admin -ca <project_name> <algorithm_name>```

  Note: to complete the algorthm creation process edit the file
  `algs/ALG-<algorithm_name>/<algorithm_name>Algorithm.java`


## Presenting the results of execution

To show the results of ALGator's execution in a web browser, you need to 

   - Install the docker for Windows (see https://docs.docker.com/install/ for details). During the instalation:

     - Use Linux containers instead of windows ones

     - If Docker asks for Hyper-V allow docker to install it

     - Enable shared drives as on picture or disk on your preference
     ​
       <img src="doc/images/docker.png" alt="Docker Shared drives" />

     ​

   - Run the command

     ```docker run --mount type=bind,source=%ALGATOR_ROOT%/data_root,target=/home/algator/ALGATOR_ROOT/data_root -p 8081:8081 algator/algatorweb```

   - Open a web browser and type in the following address

     http://localhost:8081/

   Note: in the ALGator.zip there are two example projects: BasicSort and BasicMatrixMul.
   You can browse the results of these projects before creating your own project.

   When facing a problem with shared drives, try <a href="http://peterjohnlightfoot.com/docker-for-windows-on-hyper-v-fix-the-host-volume-sharing-issue/">this.</a>
   
