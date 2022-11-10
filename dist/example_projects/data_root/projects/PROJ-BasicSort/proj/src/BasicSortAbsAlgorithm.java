
import si.fri.algotest.execute.AbstractAlgorithm;

/**
 *
 * @author tomaz
 */
public abstract class BasicSortAbsAlgorithm extends AbstractAlgorithm {

  protected abstract BasicSortOutput execute(BasicSortInput basicSortInput);
  
  @Override
  public BasicSortTestCase getCurrentTestCase() {
    return (BasicSortTestCase) super.getCurrentTestCase(); 
  }

  @Override
  public void run() {    
    algorithmOutput = execute(getCurrentTestCase().getInput());
  }
}
