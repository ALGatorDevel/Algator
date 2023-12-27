import si.fri.algator.execute.AbstractInput;

/**
 * @author ...
 */
public class BasicMatrixMulInput extends AbstractInput {

  // input matrices
  int [][] A;
  int [][] B;
  
  public BasicMatrixMulInput(int [][] A, int [][] B) {    
    this.A = A;
    this.B = B;
  }
      
  @Override
  public String toString() {
    String dataA, dataB; dataA = dataB = "";
    for (int i = 0; i < 5; i++) {
      if (i < A.length) {
        dataA += (!dataA.isEmpty() ? ", ":"") + A[0][i];
        dataB += (!dataB.isEmpty() ? ", ":"") + B[0][i];        
      }
    }
    String matrika = " %c = [%s ...] ";
    return "N = " + A.length + String.format(matrika, 'A', dataA)
                             + String.format(matrika, 'B', dataB);
  }
}