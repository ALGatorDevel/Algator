/**
 *
 * @author ...
 */
public class Algorithm extends ProjectAbstractAlgorithm {
@Override
  protected Output execute(Input input) {
    
    int n = input.A.length;
	  int [][] C = new int[n][n];
    
    for (int i = 0; i < input.A.length; i++) {
      for (int j = 0; j < input.A.length; j++) {
        for (int k = 0; k < input.A.length; k++) {
          C[i][j] += input.A[i][k] * input.B[k][j];
        }
      }
    }

    Output output = new Output(C);    
    return output;
  }
}