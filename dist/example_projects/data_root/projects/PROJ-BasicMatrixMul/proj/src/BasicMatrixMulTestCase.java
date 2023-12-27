import si.fri.algator.entities.Variables;
import si.fri.algator.execute.AbstractTestCase;
import si.fri.algator.global.ErrorStatus;

/**
 *
 * @author ...
 */
public class BasicMatrixMulTestCase extends AbstractTestCase {

  @Override
  public BasicMatrixMulInput getInput() {
    return (BasicMatrixMulInput) super.getInput(); 
  } 

  @Override
  public BasicMatrixMulOutput getExpectedOutput() {
    return (BasicMatrixMulOutput) super.getExpectedOutput();
  }
  
  
  /**
   * 
   * @param includeC if includeC==false, the result (C) is not calculated and the test of correctness will be performed based of approx. alg.
   * @return 
   */
  public BasicMatrixMulTestCase randomTestCase(Variables inputParameters, int n, long seed, boolean includeC) { 
    int [][] A = null, B=null, C=null; 
    
    String path  = inputParameters.getVariable(TESTS_PATH,    "")   .getStringValue();
    String rpath = BasicMatrixMulTools.createPathForRandomMatrices(path); // path for random matrices storage
    String matrixNameFormat = "r-%d-%d-%c"; // name of a random matrix in rPath

    // for a given (non-random) seed, matrices are stored in a file
    if (seed != -1 && rpath != null) try {      
      A = BasicMatrixMulTools.readMatrix(rpath, String.format(matrixNameFormat, n,seed,'A'));
      B = BasicMatrixMulTools.readMatrix(rpath, String.format(matrixNameFormat, n,seed,'B'));
      
      if (includeC)
        C = BasicMatrixMulTools.readMatrix(rpath, String.format(matrixNameFormat, n,seed,'C'));        
    } catch (Exception e) {}
    
     
    if (A==null || B==null || (includeC && C==null)) {
      A = BasicMatrixMulTools.createRNDMatrix(n, seed);
      B = BasicMatrixMulTools.createRNDMatrix(n, seed);
      
      if (includeC)
        C = BasicMatrixMulTools.multiply(A, B);  
    }
    
    // for a given (non-random) seed, matrices are stored in a file
    if (seed != -1 && rpath != null) try {      
      BasicMatrixMulTools.writeMatrix(A, rpath, String.format(matrixNameFormat, n,seed,'A'));
      BasicMatrixMulTools.writeMatrix(B, rpath, String.format(matrixNameFormat, n,seed,'B'));
      
      if (includeC)
        BasicMatrixMulTools.writeMatrix(C, rpath, String.format(matrixNameFormat, n,seed,'C'));
    } catch (Exception e) {}        
    
    // create a test case 
    BasicMatrixMulTestCase basicMatrixMulTestCase = new BasicMatrixMulTestCase();                
    basicMatrixMulTestCase.setInput(new BasicMatrixMulInput(A,B));    
    basicMatrixMulTestCase.getInput().setParameters(inputParameters);    
    basicMatrixMulTestCase.setExpectedOutput(new BasicMatrixMulOutput(C));
    
    return basicMatrixMulTestCase;    
  }

  
  @Override
  /**
   * TYPE0 test case generator. 
   * Method creates a test case based on the given test case parameters.
  **/
  public BasicMatrixMulTestCase testCaseGenerator(Variables inputParameters) {
    int n        = inputParameters.getVariable("N",        1000).getIntValue();              
    return randomTestCase(inputParameters, n, -1, true);
  }

  /**
   * 
   * @return randomTestCase with a given seed in which only A, B and C are set
   */
  public BasicMatrixMulTestCase testCaseGenerator1(Variables inputParameters) {
    String path  = inputParameters.getVariable(TESTS_PATH,    "")   .getStringValue();    
    int n        = inputParameters.getVariable("N").getIntValue();              
    int seed     = inputParameters.getVariable("Seed").getIntValue();            

    return randomTestCase(inputParameters, n, seed, true);
  }
  
  
  /**
   * 
   * @return randomTestCase with a given seed in which only A and B are set; the calculation of C is ommited
   */
  public BasicMatrixMulTestCase testCaseGenerator2(Variables inputParameters) {
    String path  = inputParameters.getVariable(TESTS_PATH,    "")   .getStringValue();    
    int n        = inputParameters.getVariable("N").getIntValue();              
    int seed     = inputParameters.getVariable("Seed").getIntValue();            

    return randomTestCase(inputParameters, n, seed, false);
  }
  

  @Override
  /**
   * TYPE2 test case generator. 
   * Method reads input matrices (A and B) and output matrix (C) from files. 
  **/  
  public BasicMatrixMulTestCase testCaseGenerator3(Variables inputParameters) {
    String path      = inputParameters.getVariable(TESTS_PATH,    "")   .getStringValue();              
    int N            = inputParameters.getVariable("N",        1000).getIntValue();              

    String filenameA = inputParameters.getVariable("FilenameA", "").getStringValue();              
    String filenameB = inputParameters.getVariable("FilenameB", "").getStringValue();              
    String filenameC = inputParameters.getVariable("FilenameC", "").getStringValue();              
    
    int [][] A = BasicMatrixMulTools.readMatrixS(path, filenameA);
    int [][] B = BasicMatrixMulTools.readMatrixS(path, filenameB);
    int [][] C = BasicMatrixMulTools.readMatrixS(path, filenameC);
                    
    if (A == null || B == null || C == null) {
      ErrorStatus.setLastErrorMessage(ErrorStatus.ERROR, "Invalid parameters, can not generate matrices.");
      return null;
    }
               
    BasicMatrixMulTestCase basicMatrixMulTestCase = new BasicMatrixMulTestCase();                
    basicMatrixMulTestCase.setInput(new BasicMatrixMulInput(A, B));    
    basicMatrixMulTestCase.getInput().setParameters(inputParameters);    
    basicMatrixMulTestCase.setExpectedOutput(new BasicMatrixMulOutput(C));
    
    return basicMatrixMulTestCase;
  }
}