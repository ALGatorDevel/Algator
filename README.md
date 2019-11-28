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

A single-user version instalation of the ALGator system on Linux system (see also a <a href="README_WINDOWS.md">Windows version</a> of this document):

1. **Create ALGator folder**

    We will refere to this folder as the `<algator_root>` folder

2. **Download ALGator.zip** file from GitHub

    ```
    curl -L -O https://raw.github.com/ALGatorDevel/Algator/master/ALGator.zip
    ```
    

3. **Unpack ALGator.zip** file to `<algator_root>`

	```
	$ cd <algator_root>
	$ unzip ALGator.zip
	```

 
4. **Set environment variables** 
  
   Add the following lines to `~/.bash_profile`:
  
	```
	 export ALGATOR_ROOT=<algator_root>
	 export CLASSPATH=$CLASSPATH:$ALGATOR_ROOT/app/ALGator/ALGator.jar
	```

   Note: in the first line change the `<algator_root>` with the name of 
   your ALGator folder.

5. **To test correctness** of the instalation, type

    ```java algator.Version```


   Note: in the ALGator.zip there are two ready-to-use example projects: BasicSort 
   and BasicMatrixMul (in the `data_root` folder). Use these two project as a reference 
   and as an example-driven tutorial for your projects. 



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
  
   - Install the docker (see https://docs.docker.com/install/ for details) 
  
   - Run the docker's container 
  
     ```docker run --mount type=bind,source=$ALGATOR_ROOT/data_root,target=/home/algator/ALGATOR_ROOT/data_root -p 8081:8081 algator/algatorweb```


   - Run the query-processing server 
  
     ```java algator.TaskServer```

   - Open a web browser and type in the following address
  
     http://localhost:8081/
 


## Prerequisits 

Current version of ALGator works only with Java 8.  Note: Environment variable JAVA_HOME should be set to Java 8 home.


## ALGator on GitHub

  https://github.com/ALGatorDevel/Algator



