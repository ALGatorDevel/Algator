package si.fri.algator.execute;

import java.io.File;
import si.fri.algator.entities.MeasurementType;
import si.fri.algator.entities.Project;
import si.fri.algator.entities.Variables;
import static si.fri.algator.execute.ExternalExecutor.runTestCase;
import si.fri.algator.global.ATLog;
import si.fri.algator.server.ASTask;

record TestCaseInfo(String testCaseName, String testSetName){}

/**
 *
 * @author tomaz
 */
public class UniExecute {
  
  
  public static void iterateTestSetAndRunAlgorithm(
          String projectName, String algName, String currentJobID, AbstractTestSetIterator it,
          MeasurementType mType, File resultFile, ASTask task) {

    int testID = 0; 
    int taskProgress = (task == null) ? 0 : task.getProgress();    
    
    String status = String.format("[%s, %s, %s, %s]: ", 
            projectName, algName, it.testSet.getName(), mType.getExtension());    
    
    try {
      while (it.hasNext()) {
        it.readNext(); 

        // skip all the tests that were already completed
        if (taskProgress > testID++) continue;
        
        ATLog.log(status + String.format(" --> generating testcase (%d / %d)", testID, it.testSet.getFieldAsInt("N", 0)));
        AbstractTestCase testCase = it.getCurrent();

        String testName = "";
        try {
          testName = testCase.getInput().getParameters().getVariable("Test", "").getStringValue();
        } catch (Exception e) {}
        if (testName.isEmpty()) {
          testName = "Test-" + testID;
        }
/*
        ATLog.log(status + String.format(" ... running '%s'", testName));
        Variables resultVariables = runTestCase(project, algName, testCase, currentJobID, mType,
                testSetName, testID, timesToExecute, timeLimit, testName);
*/
      }
    } catch (Exception e) {}
  }
}
