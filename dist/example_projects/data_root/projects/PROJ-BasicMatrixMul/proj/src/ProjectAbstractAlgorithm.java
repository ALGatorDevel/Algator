import si.fri.algator.execute.AbstractAlgorithm;

/**
 * 
 * @author tomaz
 */
public abstract class ProjectAbstractAlgorithm extends AbstractAlgorithm {
 
  @Override
  public TestCase getCurrentTestCase() {
    return (TestCase) super.getCurrentTestCase(); 
  }

  protected abstract Output execute(Input basicMatrixMulInput);

  @Override
  public void run() {    
    algorithmOutput = execute(getCurrentTestCase().getInput());
  }
}