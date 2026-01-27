package algator;

import java.net.URL;
import si.fri.algator.entities.EVariable;
import si.fri.algator.entities.Project;
import si.fri.algator.entities.Variables;
import si.fri.algator.execute.AbstractTestCase;
import si.fri.algator.execute.New;
import si.fri.algator.global.ATGlobal;

/**
 *
 * @author tomaz
 */
public class GenerateTestcase {
    
  public static void main(String[] args) {
    AbstractTestCase testCase = AbstractTestCase.generateTestcase(args[0], args[1]);
    System.out.println(testCase.toJSONString());
  }
}
