package algator;

import si.fri.algator.entities.EVariable;
import si.fri.algator.entities.MeasurementType;
import si.fri.algator.entities.Project;
import si.fri.algator.entities.Variables;
import si.fri.algator.execute.Executor;
import si.fri.algator.execute.ExternalExecutor;
import si.fri.algator.global.ATGlobal;

/**
 *
 * @author tomaz
 */
public class RunOneExample {
  
  public static void main(String[] args) {
    String data_root = ATGlobal.getALGatorDataRoot();
    
    
    String projName      = "BasicSort";         // Project name
    String algName       = "QuickSort";         // Algorithm name
    MeasurementType mt   = MeasurementType.EM;  // Values: EM, CNT, JVM (default: EM)
    String generatorType = "Type0";             // Generator type to be used
    int timeLimit        = 1;                   // Maximum allowed execution time (in seconds)
    int timesToExecute   = 1;                   // Number of times to execute the algorithm

    Project project = new Project(data_root, projName);    

    // execute exactly once (not in a loop)
    Executor.projectMakeCompile(data_root, projName, false, null);
    Executor.algorithmMakeCompile(data_root, project.getName(), algName, MeasurementType.EM, false);

    Variables parameters = new Variables();


    int n=1000;
    int tmin = 0;
    while (tmin < 5000) {
      parameters.addVariable(new EVariable("N", n));
      Variables result = ExternalExecutor.runParametrizedTest(project, algName, mt, generatorType, parameters, timeLimit, timesToExecute);
      tmin = 0; try {tmin = (int) result.getVariable("Tmin").getLongValue() / 1000;} catch (Exception e) {}
      System.out.printf("N=%10d, Tmin=%d ms\n", n, tmin);
      n*=2;
    }
  }
} 
