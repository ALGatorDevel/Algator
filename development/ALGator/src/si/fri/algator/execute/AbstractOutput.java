package si.fri.algator.execute;

import java.io.Serializable;
import java.lang.reflect.Method;
import si.fri.algator.entities.EVariable;
import si.fri.algator.entities.Variables;


/**
 * An output of an algorithm. 
 * @author tomaz
 */
public abstract class AbstractOutput implements Serializable {

  protected Variables indicators;

  public AbstractOutput() {
    indicators = new Variables();
  }
  
  public String toString() {
    return this.getClass().getName();
  }
  
  public void addIndicator(EVariable indicator) {
    addIndicator(indicator, true);
  }
  
  public void addIndicator(EVariable indicator, boolean replaceExisting) {
    indicators.addVariable(indicator, replaceExisting);
  }
  
  public Variables getIndicators() {
    return indicators;
  }
  
  
  // create IndicatorTest_<indicator_name> class and run its getValue() method
  protected Object getIndicatorValue(AbstractTestCase testCase, AbstractOutput algorithmOutput, String indicatorName) 
  {
    try {
      ClassLoader cl =  testCase.getClass().getClassLoader(); // get classloader that loaded testcase
      Class testIndicator = Class.forName("IndicatorTest_" + indicatorName, true, cl);
      AbstractIndicatorTest ait = (AbstractIndicatorTest)testIndicator.newInstance();
      Method getValue = testIndicator.getDeclaredMethod("getValue", testCase.getClass(), algorithmOutput.getClass());
      return getValue.invoke(ait, testCase, algorithmOutput);
    } catch (Exception e) {
      return "?";
    }
  }  
  
}
