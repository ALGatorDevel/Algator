package si.fri.algator.execute;

import java.io.Serializable;
import java.util.HashMap;
import si.fri.algator.entities.EVariable;
import si.fri.algator.entities.MeasurementType;
import si.fri.algator.entities.Variables;
import si.fri.algator.global.ATGlobal;
import si.fri.algator.global.ATLog;
import si.fri.algator.global.ErrorStatus;
import si.fri.algator.timer.Timer;

/**
 *
 * @author tomaz
 */
public abstract class AbstractAlgorithm implements Cloneable, Serializable {
  private final static String TMP_FOLDER_PREFIX = "exec";
  
  // This data is needed by ExternalExecutor to determine the type of execution.
  private MeasurementType mType;
  
  public Timer timer;                 // timer to measure the execution time of the current test
  private long[][] executoinTimes;    // the times of execution for all executions of the current test
  
  // values of counters after the execution of algorithm
  private HashMap<String, Integer> counters = new HashMap();
  
  private String workingFolder = null;
  
  
  AbstractTestCase currentTestCase;
  protected AbstractOutput   algorithmOutput;
  
  
  public AbstractAlgorithm() {
    timer = new Timer();
    executoinTimes = new long[0][0];
    counters = new HashMap<>();
  }
  
  
  public int getTimesToExecute() {
    if (executoinTimes != null && executoinTimes.length > 0)
      return executoinTimes[0].length;
    else return 0;
  }
  
  public void setTimesToExecute(int timesToExecute) {
    executoinTimes = new long[Timer.MAX_TIMERS][timesToExecute];
  }
    
  public long [][] getExecutionTimes() {
    return executoinTimes;
  }
  
  public void setExectuionTime(int timer, int executionID, long time) {
    if (timer < executoinTimes.length && executionID < executoinTimes[timer].length)
      executoinTimes[timer][executionID] = time;
  }
 
  public HashMap<String, Integer> getCounters() {
    return counters;
  }
  
  public void setCounters(HashMap<String, Integer> counters) {
    this.counters = (HashMap<String, Integer>) counters.clone();
  }

  public MeasurementType getmType() {
    return mType;
  }

  public void setmType(MeasurementType mType) {
    this.mType = mType;
  }
  
  
  public String getWorkingFolder() {
    if (workingFolder == null)
      workingFolder = ATGlobal.getTMPDir(TMP_FOLDER_PREFIX);
    return workingFolder;
  }
  
  /**
   * Closing algoritm. Currently this method delets working folder if it was created.
   */
  public void close() {
    if (workingFolder != null)
      ATGlobal.deleteTMPDir(workingFolder, TMP_FOLDER_PREFIX);
  }
  
  
  /**
   * Extract data from {@code test} and prepare them in the form to be simply used
   * when running execute method of [Alg]Algorithm.execute().
   */
  public ErrorStatus init(AbstractTestCase test) {
    currentTestCase = test;
    
    return ErrorStatus.STATUS_OK;
  }
  
  /**
   * Execute the [Alg]Algorithm.execute() method with the prepared data. 
   * The time-overhead of this method (time used before and after calling the execute()  
   * method) should be as small as possible, since the execution time of this method 
   * is measured as execution time of the algorithm.
   */
  public abstract void run();
  
  /**
   * This metod is called after the method run() to collect the result data and to 
   * verify the correctness of the solution. 
   */
  public Variables done() {
    Variables result = new Variables(currentTestCase.getDefaultOutput().getIndicators());
     
    for (EVariable eVariable : result) {      
      Object value = null;
      try {
        value = AbstractOutput
           .getIndicatorValue(currentTestCase, algorithmOutput, eVariable.getName());
      } catch (Exception e) {
        ATLog.log("Invalid result " + e.toString(), 1);
      }
      
      if (value != null) {
        eVariable.setValue(value);
      }
    }
    return result;
  }  
  
  /**
   * This methods returns the instance bundle that is attached to this algorithm.
   */
  public  AbstractTestCase getCurrentTestCase() {
    return currentTestCase;
  }
  
  
  public AbstractInput getCurrentInput() {
    if (getCurrentTestCase() != null)
      return getCurrentTestCase().getInput();
    return null;
  }  
  
  @Override
  public Object clone() throws CloneNotSupportedException {
   return super.clone();
  }
}
