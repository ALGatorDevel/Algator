import si.fri.algotest.execute.AbstractAlgorithm;

/**
 *
 * @author ...
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