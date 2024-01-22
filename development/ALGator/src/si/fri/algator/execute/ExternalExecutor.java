package si.fri.algator.execute;

import algator.ExternalExecute;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import org.apache.commons.lang3.ArrayUtils;
import org.json.JSONObject;
import si.fri.algator.server.ASTask;
import si.fri.algator.client.Requester;
import si.fri.algator.entities.ELocalConfig;
import si.fri.algator.entities.EVariable;
import si.fri.algator.entities.EResult;
import si.fri.algator.entities.ETestSet;
import si.fri.algator.entities.MeasurementType;
import si.fri.algator.entities.Variables;
import si.fri.algator.entities.VariableType;
import si.fri.algator.entities.Project;
import si.fri.algator.entities.StatFunction;
import si.fri.algator.global.ATGlobal;
import si.fri.algator.global.ATLog;
import si.fri.algator.global.ErrorStatus;
import si.fri.algator.global.ExecutionStatus;
import si.fri.algator.tools.UniqueIDGenerator;

/**
 *
 * @author tomaz
 */
public class ExternalExecutor {

  /**
   * The original algorithm that is passed to the executor is written to the
   * TEST file, the algorithm with the results (times and parameters) is written
   * to the RESUL file.
   */
  public static enum SER_ALG_TYPE {

    TEST {
      @Override
      public String toString() {
        return ".test";
      }
    },
    RESULT {
      @Override
      public String toString() {
        return ".result";
      }
    }
  }

  private final static String SERIALIZED_ALG_FILENAME = "alg.ser";

  private static final EVariable failedErr = EResult.getExecutionStatusIndicator(ExecutionStatus.FAILED);

  /**
   * This file is used as a communication chanell between main ALGator executor
   * and ExternalJVMExecutor. When an algorithm test is started, this file is
   * initialized with empty contents. For each execution of the algorithm,
   * ExternalJVMExecutor writes one byte to this file. ALGator's executor
   * regulary checks the content of this file and stops the execution is there
   * is no progress.
   */
  private final static String COMMUNICATION_FILENAME = "comm.data";

  /**
   * Iterates through testset and for each test runs an algorithm.
   *
   * @param project
   * @param algName
   * @param it
   * @param resultDesc
   * @param notificator
   * @param mType
   * @param task ... if task != null, we are running as TaskClient; in this case, we proceed from task.i test on; after each test, we send data to server; server telll us, if we should go on with tests or stop  
   * @return
   */
  public static void iterateTestSetAndRunAlgorithm(Project project, String algName, String currentJobID, AbstractTestSetIterator it,
          Notificator notificator, MeasurementType mType, File resultFile, int whereToPrint, boolean asJSON, ASTask task) {

    String instanceID = "Test"; //UniqueIDGenerator.getNextID();

    ETestSet testSet = it.testSet;

    // get the number of times to execute one test (this is applicable only
    /// for the EM type of tests; all other tests are performed only once)
    int timesToExecute = 1;
    if (mType.equals(MeasurementType.EM)) {
      try {
        timesToExecute = testSet.getFieldAsInt(ETestSet.ID_TestRepeat, 1);
      } catch (Exception e) {
        // if ETestSet.ID_TestRepeat parameter is missing, timesToExecute is set to 1 and exception is ignored
      }
    }
    timesToExecute = Math.max(timesToExecute, 1); // to prevent negative number

    // Maximum time allowed (in seconds) for one execution of one test; if the algorithm 
    // does not  finish in this time, the execution is killed
    int timeLimit = 10;
    try {
      timeLimit = testSet.getFieldAsInt(ETestSet.ID_TimeLimit, 10);
    } catch (Exception e) {
      try {
        timeLimit = Integer.parseInt(testSet.getField(ETestSet.ID_TimeLimit).toString());
      } catch (Exception ex) {
        // if ETestSet.ID_TimeLimit parameter is missing or invalid, timelimit is set to 10 (sec) and exception is ignored
      }
    }

    EResult resultDesc = project.getResultDescriptions().get(mType);
    if (resultDesc == null) {
      resultDesc = new EResult();
    }

    int taskProgress = 0;
    if (task != null) 
      taskProgress = task.getProgress();
    
    int testID = 0; 
    ErrorStatus eStatus = ErrorStatus.STATUS_OK;
    
    String algatorServerName = ELocalConfig.getConfig().getALGatorServerName();
    int    algatorServerPort = ELocalConfig.getConfig().getALGatorServerPort();

    try {
      while (it.hasNext()) {
        it.readNext();

        // skip all the tests that were already completed
        if (taskProgress > testID++) continue;
        

        if (ATGlobal.verboseLevel == 2) {
          System.out.print("\rGenerating testcase...");
          System.out.flush();
        }
        AbstractTestCase testCase = it.getCurrent();

        if (ATGlobal.verboseLevel == 2) {
          System.out.print("\r                      ");
          System.out.flush();
        }

        String testSetName = it.testSet.getName();

        String testName = "";
        try {
          testName = testCase.getInput().getParameters().getVariable("Test", "").getStringValue();
        } catch (Exception e) {
        }
        if (testName.isEmpty()) {
          testName = instanceID + "-" + testID;
        }

        if (ATGlobal.verboseLevel == 2) {
          System.out.print("\rRunning algorithm...");
          System.out.flush();
        }

        Variables resultVariables = runTestCase(project, algName, testCase, currentJobID, mType,
                testSetName, testID, timesToExecute, timeLimit, notificator, testName);

        if (ATGlobal.verboseLevel == 2) {
          System.out.print("\r                     \r");
          System.out.flush();
        }

        printVariables(resultVariables, resultFile, EResult.getVariableOrder(project.getTestCaseDescription(), resultDesc), whereToPrint, asJSON);
        
        taskProgress++;
        if (task!= null) {
          resultVariables.addVariable(new EVariable("TaskID", task.getTaskID()));
          // obvesti server -> pošlji mu podatke: taskID, computerUID, taskProgress ter result 
          String[] order = EResult.getVariableOrder(project.getTestCaseDescription(), resultDesc);
          if (asJSON) {order = Arrays.copyOf(order, order.length+1);order[order.length-1]="TaskID";}
          String result = 
            resultVariables.toString(order, asJSON, ATGlobal.DEFAULT_CSV_DELIMITER);
                    
          JSONObject jObj = new JSONObject();
          jObj.put(ASTask.ID_ComputerUID, task.getComputerUID());
          jObj.put(ASTask.ID_TaskID, task.getTaskID());
          jObj.put("TestNo", testID);
          jObj.put("Result", result);
          String sResponse = Requester.askALGatorServer(
              algatorServerName, algatorServerPort, "TASKRESULT " + jObj.toString());
          
          // od serverja dobiš odgovor: CONTINUE ali QUEUED ali BREAK ali napako
          // samo odgovor CONTUNUE pomeni nadaljevanje, vse ostalo pa sproži
          // prekinitev izvajanja tega testseta
          boolean cont = false;
          try {
            JSONObject jAns = new JSONObject(sResponse);
            if (jAns.getInt("Status")==0 && jAns.getString("Answer").equals("CONTINUE"))
              cont = true;
          } catch (Exception e) {}
          
          if (!cont) {
            eStatus = ErrorStatus.PROCESS_QUEUED;
            break;
          }
        }
      }
      it.close();
    } catch (Exception e) {
      ErrorStatus.setLastErrorMessage(ErrorStatus.ERROR_CANT_RUN, e.toString());
      return;
    }

    ErrorStatus.setLastErrorMessage(eStatus, "");
  }

  public static Variables runTestCase(
          Project project, String algName, AbstractTestCase testCase, String currentJobID, MeasurementType mType,
          String testSetName, int testID, int timesToExecute, int timeLimit,
          Notificator notificator, String instanceID) {

    if (instanceID == null || instanceID.isEmpty()) {
      instanceID = UniqueIDGenerator.getNextID();
    }
    String algClassName = project.getAlgorithms().get(algName).getAlgorithmClassname();
    AbstractAlgorithm algorithm = New.algorithmInstance(currentJobID, algClassName, mType);
    AbstractInput input = testCase != null ? testCase.getInput() : null;

    EResult resultDesc = project.getResultDescriptions().get(mType);
    if (resultDesc == null) {
      resultDesc = new EResult();
    }

    // V testcase dodam podatke o indikatorjih, da se ohrani informacija, ki je bila definirana
    // v atrd datoteki; recimo, če je tam definiram indikator tipa double, se prej podatki o tem indikatorju (recimo 
    // meta - število decimalk) ni prenesel in se je zato vedno uporabila default vrednost. Po tej spremembi se 
    // podatki pravilno prenesejo.
    // ??? bi moral prenesti tudi parametre? Namesto resultDesc.getVariables() bi pisal join(project.getTestCaseDescription.getParameters, resultDesc.getVariables())
    if (testCase != null) {
      AbstractOutput defaultOutput = testCase.getDefaultOutput();
      for (EVariable evar : resultDesc.getVariables()) {
        defaultOutput.addIndicator(evar, false);
      }
    }

    // nastavim podatek o številu ponovitev
    if (algorithm != null) {
      algorithm.setTimesToExecute(timesToExecute);
    }

    // were instanceBundle and algorithm created?
    boolean executionOK = testCase != null && algorithm != null;

    // was algorithm properly initialized?
    executionOK = executionOK && algorithm.init(testCase) == ErrorStatus.STATUS_OK;

    String cFolderName = ATGlobal.getTMPDir(project.getName());
    Variables resultVariables = new Variables();

    try {
      ErrorStatus executionStatus = ErrorStatus.ERROR_CANT_PERFORM_TEST;

      if (executionOK) {
        saveAlgToFile(New.getClassPathsForProjectAlgorithm(project, algName), algorithm, cFolderName, SER_ALG_TYPE.TEST);

        executionStatus = runWithLimitedTime(cFolderName, timesToExecute, timeLimit, mType, false);
      }

      EVariable executionStatusParameter;
      switch (executionStatus) {
        case STATUS_OK:
          if (notificator != null) {
            notificator.notify(testID, ExecutionStatus.DONE);
          }
          executionStatusParameter = EResult.getExecutionStatusIndicator(ExecutionStatus.DONE);
          break;
        case PROCESS_KILLED:
          if (notificator != null) {
            notificator.notify(testID, ExecutionStatus.KILLED);
          }
          executionStatusParameter = EResult.getExecutionStatusIndicator(ExecutionStatus.KILLED);
          break;
        default:
          if (notificator != null) {
            notificator.notify(testID, ExecutionStatus.FAILED);
          }
          executionStatusParameter = failedErr;
      }

      Variables algResultIndicators = new Variables();

      // algorithm instance obtained from file as a result of execution
      if (executionStatus == ErrorStatus.STATUS_OK) { // the execution passed normaly (not killed)
        // pridobim classLoader, ki je naložil "algorithm"; ker ta loader že obstaja, lahko URLje izpustim (dobil bom odgovor iz slovarja)
        ClassLoader cl = New.getClassloader(currentJobID);
        AbstractAlgorithm resultAlg = getAlgorithmFromFile(cFolderName, SER_ALG_TYPE.RESULT, cl);

        if (resultAlg != null) {
          algResultIndicators = resultAlg.done();

          switch (mType) {
            case EM:
              algResultIndicators.addVariables(getTimeIndicators(resultDesc, resultAlg), true);
              break;
            case CNT:
              algResultIndicators.addVariables(getCounterIndicators(resultDesc, resultAlg), true);
              break;
          }
        } else {
          algResultIndicators.addVariable(EResult.getErrorIndicator("Invalid test: " + input.toString()), true);
          executionStatusParameter = failedErr;
        }
      } else { // the execution did not perform succesfully          
        try {
          // if possible, obtain indicators from the algorithm 
          algResultIndicators = algorithm.getCurrentTestCase().getDefaultOutput().getIndicators();
        } catch (Exception e) {
          algResultIndicators = new Variables();
        }

        if (executionStatus == ErrorStatus.PROCESS_KILLED) {
          algResultIndicators.addVariable(EResult.getErrorIndicator(
                  String.format("Process killed after %d seconds.", timeLimit)), true);
        } else {
          algResultIndicators.addVariable(EResult.getErrorIndicator(ErrorStatus.getLastErrorMessage()), true);
        }
      }

      try { // !!! TU NE BI SMELO BITI NAPAKE! To se dogaja zaradi java 9, ker očitno uporabljam različne 
        // classloaderje (tu dobim napako  "class BasicSortTestCase can not be cast to BasicSortTestCase") 
        if (algorithm.getCurrentInput() != null) {
          resultVariables.addVariables(algorithm.getCurrentInput().getParameters());
        }
      } catch (Exception e) {
        //System.out.println(e.toString());
      }

      resultVariables.addVariables(algResultIndicators, false);
      resultVariables.addVariable(EResult.getAlgorithmNameParameter(algName), true);
      resultVariables.addVariable(EResult.getTestsetNameParameter(testSetName), true);
      resultVariables.addVariable(EResult.getTimestampParameter(System.currentTimeMillis()), true);
      resultVariables.addVariable(EResult.getInstanceIDParameter(instanceID), true);

      resultVariables.addVariable(executionStatusParameter, true);

    } catch (Exception e) {
    } finally {
      ATGlobal.deleteTMPDir(cFolderName, project.getName());
    }

    return resultVariables;

  }

  /**
   * Prints varibles (parameters and indicators) in a given order to stdout
   * and/or file. Parameter whereToPrint: 0 ... none, 1 ... stdout, 2 ... file
   * (note: 3 = both, 1 and 2).
   */
  public static void printVariables(Variables resultVariables, File resultFile, String[] order, int whereToPrint, boolean asJSON) {
    if (resultVariables != null) {
      // print to stdout
      if (((whereToPrint & ATLog.TARGET_STDOUT) == ATLog.TARGET_STDOUT)) {
        resultVariables.printToFile(new PrintWriter(System.out), order, true, asJSON);
      }

      // print to file
      if (((whereToPrint & ATLog.TARGET_FILE) == ATLog.TARGET_FILE) && (resultFile != null)) {
        resultVariables.printToFile(resultFile, order, asJSON);
      }
    }
  }

  // has the process finished?
  private static boolean processIsTerminated(Process process) {
    try {
      process.exitValue();
    } catch (IllegalThreadStateException itse) {
      return false;
    }
    return true;
  }

  /**
   * Method runs a given algorithm (algorithm's serialized file is written in
   * foldername) for n times (where n is one of the paramters in the algorithm's
   * testcase) and returns null if each execution finished in
   * timeForOneExecutionTime and errorMessage otherwise
   */
  static ErrorStatus runWithLimitedTime(String folderName, int timesToExecute,
          long timeForOneExecution, MeasurementType mType, boolean verbose) {

    Object result = ExternalExecute.runWithExternalJVM(folderName, mType, verbose);

    // during the process creation, an error occured
    if (result instanceof String) {
      return ErrorStatus.setLastErrorMessage(ErrorStatus.PROCESS_CANT_BE_CREATED, "Err a:" + (String) result);
    }

    if (!(result instanceof Process)) {
      return ErrorStatus.setLastErrorMessage(ErrorStatus.ERROR.ERROR, "?unknown?");
    }

    Process externProcess = (Process) result;

    long milis = System.currentTimeMillis();
    whileloop:
    while (true) {
      // loop for one second
      for (int i = 0; i < 10; i++) {
        if (processIsTerminated(externProcess)) {
          break whileloop;
        }
        // wait for 0.1s
        try {
          Thread.sleep(100);
        } catch (Exception e) {
        }
      }

      long resultsCount = getCommunicationCount(folderName);
      long secondsPassed = (System.currentTimeMillis() - milis) / 1000;

      int expectedResults = (int) (secondsPassed / timeForOneExecution);

      //!!
      //System.out.printf("seccondsPassed=%d, timeForOne=%d, ResultCount= %d, expectedResults=%d\n", secondsPassed, timeForOneExecution, resultsCount, expectedResults);
      if (resultsCount < expectedResults) {
        externProcess.destroy();
        return ErrorStatus.setLastErrorMessage(ErrorStatus.PROCESS_KILLED,
                String.format("(after %d sec.)", (int) secondsPassed));
      }
    }

    try {
      BufferedReader stdInput = new BufferedReader(new InputStreamReader(externProcess.getInputStream()));
      BufferedReader stdError = new BufferedReader(new InputStreamReader(externProcess.getErrorStream()));

      String s;
      StringBuffer sb = new StringBuffer();
      while ((s = stdInput.readLine()) != null) {
        sb.append(s);
      }
      while ((s = stdError.readLine()) != null) {
        sb.append(s);
      }

      if (sb.length() != 0) {
        return ErrorStatus.setLastErrorMessage(ErrorStatus.PROCESS_CANT_BE_CREATED, "Err b:" + sb.toString());
      } else {
        return ErrorStatus.STATUS_OK;
      }

    } catch (Exception e) {
      StringWriter errors = new StringWriter();
      e.printStackTrace(new PrintWriter(errors));
      return ErrorStatus.setLastErrorMessage(ErrorStatus.PROCESS_CANT_BE_CREATED, "Err c:" + errors.toString());
    }
  }

  // pregledam resultDesc parametre in za vsak parameter tipa "timer" ustvarim
  // parameter v results s pravo vrednostj
  static Variables getTimeIndicators(EResult resultDesc, AbstractAlgorithm algorithm) {
    Variables timeParameters = new Variables();
    long[][] times = algorithm.getExecutionTimes();

    if (resultDesc != null) {
      for (EVariable rdP : resultDesc.getVariables()) {
        if (VariableType.TIMER.equals(rdP.getType())) {
          try {
            int tID = rdP.getMeta("ID", 0);
            String statDesc = rdP.getMeta("STAT", "MIN");
            StatFunction fs = StatFunction.getStatFunction(statDesc);

            // times[tID] -> ArrayList<Long> (list)
            Long[] longObjects = ArrayUtils.toObject(times[tID]);
            ArrayList<Long> list = new ArrayList<>(java.util.Arrays.asList(longObjects));

            Object time = StatFunction.getFunctionValue(fs, list);
            VariableType vt = VariableType.TIMER;
            if (StatFunction.ALL.toString().equals(statDesc)) {
              vt = VariableType.STRING;
            }
            EVariable timeP = new EVariable((String) rdP.getName(), null, vt, time);
            timeParameters.addVariable(timeP, true);
          } catch (Exception e) {
            ErrorStatus.setLastErrorMessage(ErrorStatus.ERROR, "Meta parameter invalid (" + e.toString() + ")");
          }
        }
      }
    }
    return timeParameters;
  }

  static Variables getCounterIndicators(EResult resultDesc, AbstractAlgorithm algorithm) {
    Variables counterParameters = new Variables();
    HashMap<String, Integer> counters = algorithm.getCounters();
    if (resultDesc != null && counters != null) {
      for (EVariable evar : resultDesc.getVariables()) {
        if (VariableType.COUNTER.equals(evar.getType())) {
          String counterName = (String) evar.getName();
          int value = 0;
          if (counters.containsKey(counterName)) {
            value = counters.get(counterName);
          }
          EVariable nev = new EVariable(counterName, null, VariableType.COUNTER, value);
          counterParameters.addVariable(nev, true);
        }
      }
    }
    return counterParameters;
  }

  /**
   * Saves the measurement type, classpath and algotihm instance to a file.
   */
  public static boolean saveAlgToFile(URL[] urls, AbstractAlgorithm curAlg,
          String folderName, SER_ALG_TYPE algType) {
    try ( FileOutputStream fis = new FileOutputStream(new File(folderName + File.separator + SERIALIZED_ALG_FILENAME + algType));  ObjectOutputStream dos = new ObjectOutputStream(fis);) {
      dos.writeObject(urls);
      dos.writeObject(curAlg);

      return true;
    } catch (Exception e) {
      ErrorStatus.setLastErrorMessage(ErrorStatus.CANT_WRITE_ALGORITHM_TO_FILE, e.toString());
      return false;
    }
  }

  // Java 9, Java 11, .... napaka: Base ClassLoader No Longer from URLClassLoader
  // prej je spodnja metoda lepo delala, sedaj ne
  // glej https://community.oracle.com/thread/4011800 za več razlageh
  //
  // - spodnjo kodo bo treba popraviti, če želimo, da program dela tudi v Java 9 in naprej
  //
  // - poišči tudi ostale kode, kjer uporabljam URLClassLoader
  //
  //need to do add path to Classpath with reflection since the URLClassLoader.addURL(URL url) method is protected:
//  static void addPath(URL s) throws Exception {
//    URLClassLoader urlClassLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
//    Class<URLClassLoader> urlClass = URLClassLoader.class;
//    Method method = urlClass.getDeclaredMethod("addURL", new Class[]{URL.class});
//    method.setAccessible(true);
//    method.invoke(urlClassLoader, new Object[]{s});
//  }
  
  // kadar ClassLoader ni pomemben (t.j., lahko uporabim katerikoli loader, ki ima prav nastavljen
  // classpath, bom uporabil to metodo; če pa je pomemben (ker hočem, da je algoritem naložen z istim 
  // nalagalnikom kot nek drug algoritem), potem nalagalnik prinesem s seboj
  public static AbstractAlgorithm getAlgorithmFromFile(String folderName, SER_ALG_TYPE algType) {
    ClassLoader cl = new URLClassLoader(getURLsFromFile(folderName, algType));
    return getAlgorithmFromFile(folderName, algType, cl);
  }

  public static AbstractAlgorithm getAlgorithmFromFile(String folderName, SER_ALG_TYPE algType, ClassLoader cl) {
    try ( FileInputStream fis = new FileInputStream(new File(folderName + File.separator + SERIALIZED_ALG_FILENAME + algType));  ObjectInputStream ois = new ObjectInputStream(fis) {
      protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
        try {
          return Class.forName(desc.getName(), true, cl);
        } catch (Exception e) {
        }
        // ob napaki uporabi defulat resolve
        return super.resolveClass(desc);
      }
    }) {

      // skip the "used URLs" record
      Object o = ois.readObject();

      // Try to instantiate the algorithm
      o = ois.readObject();
      return (AbstractAlgorithm) o;
    } catch (Exception e) {
      ErrorStatus.setLastErrorMessage(ErrorStatus.CANT_READ_ALGORITHM_FROM_FILE, e.toString());
      return null;
    }
  }

  /*  
  public static AbstractAlgorithm getAlgorithmFromFile(String folderName, SER_ALG_TYPE algType) {    
    try (FileInputStream fis = new FileInputStream(new File(folderName + File.separator + SERIALIZED_ALG_FILENAME + algType));
            ObjectInputStream ois = new ObjectInputStream(fis)) {
      // get the URLs that were used to load algorithm ...
      Object o = ois.readObject();
      URL[] urls = (URL[]) o;
      
      // ker ne morem predpostaviti, da je ClassLoader tipa URLClassLoader, 
      // te motode ne morem uporabiti ...
      // ... and add these URLS to URLClassLoader
      if (urls != null) {
        for (URL url : urls) {
          addPath(url);
        }
      }
      
      // Try to instantiate the algorithm
      o = ois.readObject();
      return (AbstractAlgorithm) o;
    } catch (Exception e) {
      ErrorStatus.setLastErrorMessage(ErrorStatus.CANT_READ_ALGORITHM_FROM_FILE, e.toString());
      return null;
    }
  }
   */
  public static URL[] getURLsFromFile(String folderName, SER_ALG_TYPE algType) {
    try ( FileInputStream fis = new FileInputStream(new File(folderName + File.separator + SERIALIZED_ALG_FILENAME + algType));  ObjectInputStream ois = new ObjectInputStream(fis);) {
      // get the URLs that were used to load algorithm ...
      Object o = ois.readObject();
      return (URL[]) o;
    } catch (Exception e) {
      ErrorStatus.setLastErrorMessage(ErrorStatus.CANT_READ_ALGORITHM_FROM_FILE, e.toString());
      return null;
    }
  }

  /**
   * This method clears the contents of the communication file
   *
   * @param folderName
   */
  public static boolean initCommunicationFile(String folderName) {
    File f = new File(folderName + File.separator + COMMUNICATION_FILENAME);
    try ( FileWriter fw = new FileWriter(f)) {
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  public static void addToCommunicationFile(String folderName) {
    File f = new File(folderName + File.separator + COMMUNICATION_FILENAME);

    try ( FileWriter fw = new FileWriter(f, true)) {
      fw.write((byte) 0);
    } catch (Exception e) {
    }
  }

  public static long getCommunicationCount(String folderName) {
    File f = new File(folderName + File.separator + COMMUNICATION_FILENAME);
    try {
      return f.length();
    } catch (Exception e) {
      return 0;
    }
  }

  void a(Object a) {
  }

  void a(String a) {
  }

}
