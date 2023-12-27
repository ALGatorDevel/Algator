import si.fri.algator.execute.AbstractOutput;

/**
 * 
 * @author ...
 */
public class BasicMatrixMulOutput extends AbstractOutput {

  // the product of the input matrices
  int [][] C;
  
  public BasicMatrixMulOutput(int [][] C) {    
    this.C = C;
  }

  @Override
  public String toString() {
    String dataC = "";
    for (int i = 0; i < 5; i++) {
      if (i < C.length) {
        dataC += (!dataC.isEmpty() ? ", ":"") + C[0][i];
      }
    }
    String matrika = " %c = [%s ...] ";
    return "N = " + C.length + String.format(matrika, 'C', dataC);
  }

}





  
  
 
  
  
 
