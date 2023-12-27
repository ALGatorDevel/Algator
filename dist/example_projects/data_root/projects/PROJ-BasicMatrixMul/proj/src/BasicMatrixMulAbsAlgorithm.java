import si.fri.algator.execute.AbstractAlgorithm;

/**
 *
 * @author tomaz 
 */
public abstract class BasicMatrixMulAbsAlgorithm extends AbstractAlgorithm {
 
  @Override
  public BasicMatrixMulTestCase getCurrentTestCase() {
    return (BasicMatrixMulTestCase) super.getCurrentTestCase(); 
  }

  protected abstract BasicMatrixMulOutput execute(BasicMatrixMulInput basicMatrixMulInput);

  @Override
  public void run() {    
    algorithmOutput = execute(getCurrentTestCase().getInput());
  }
}