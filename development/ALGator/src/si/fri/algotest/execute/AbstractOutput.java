package si.fri.algotest.execute;

import java.io.Serializable;
import si.fri.algotest.entities.EVariable;
import si.fri.algotest.entities.Variables;


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
  
  
  protected abstract Object getIndicatorValue(
    AbstractTestCase testCase, AbstractOutput   algorithmOutput, String indicatorName);
}
