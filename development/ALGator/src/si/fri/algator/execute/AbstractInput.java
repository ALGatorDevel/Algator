package si.fri.algator.execute;

import java.io.Serializable;
import si.fri.algator.entities.EVariable;
import si.fri.algator.entities.Variables;


/**
 * An input of an algorithm
 * @author tomaz
 */
public abstract class AbstractInput implements Serializable {

  protected Variables parameters;

  public AbstractInput() {
    parameters = new Variables();
  }

  
  public String toString() {
    return this.getClass().getName();
  }
  
  
  public void addParameter(EVariable parameter) {
    addParameter(parameter, true);
  }
  
  public void addParameter(EVariable parameter, boolean replaceExisting) {
    parameters.addVariable(parameter, replaceExisting);
  }
  
  public Variables getParameters() {
    return parameters;
  }
  
  public void setParameters(Variables parameters) {
    this.parameters = parameters;
  }  
}
