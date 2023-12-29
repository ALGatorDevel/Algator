/**
 *
 * @author lukap
 */
public class Algorithm extends ProjectAbstractAlgorithm {

  @Override
  protected Output execute(Input input) {


    int n = input.A.length;
	int [][] C = new int[n][n];

	winograd(input.A, input.B, C);
    
    Output output = new Output(C);    
    return output;    
  }

  public void winograd(int [][] A, int [][] B, int [][] C){      
    int len = A.length;
    
    int []   Y = new int[len];
    int []   Z = new int[len];
    
    for (int i = 0; i < len; i++) {
        for (int j = 0; j < len; j++) {
            for (int k = 0; k < (len / 2); k++) {
                C[i][j] += (A[i][2 * k] + B[2 * k + 1][j]) * (B[2 * k][j] + A[i][2 * k + 1]);
            }
        }
    }    
    for (int i = 0; i < len; i++){
        for (int j = 0; j < (len / 2); j++) {
            Y[i] += A[i][2 * j] * A[i][2 * j + 1];
            Z[i] += B[2 * j + 1][i] * B[2 * j][i];
        }
    }    
    for (int i = 0; i < len; i++){
        for (int j = 0; j < len; j++) {
            C[i][j] = C[i][j] - Y[i] - Z[j];
        }
    }    
  }  



}