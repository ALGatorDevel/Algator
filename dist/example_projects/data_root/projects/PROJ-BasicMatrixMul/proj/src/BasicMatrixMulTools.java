
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Random;
import si.fri.algotest.tools.ARandom;
import java.util.Scanner;

/**
 * A class for common static methods (tools) of the project
 *
 * @author tomaz
 */
public class BasicMatrixMulTools {

  // maximal element value
  private static int maxValue = 10000;

  public static boolean matrixEquals(int[][] A, int[][] B) {
    if (A == null || B == null) {
      return false;
    }
    if (A.length == 0) {
      return false;
    }

    if (A.length != B.length || A[0].length != B[0].length) {
      return false;
    }

    for (int i = 0; i < A.length; i++) {
      for (int j = 0; j < A[i].length; j++) {
        if (A[i][j] != B[i][j]) {
          return false;
        }
      }
    }
    return true;
  }

  public static int[][] readMatrix(String path, String fileName) {
    try {     
      File mFile = new File(path + File.separator + fileName);
      DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(mFile)));
      int nSq = dis.readInt();
      int n = (int) Math.round(Math.sqrt(nSq));
      int[][] result = new int[n][n];
      for (int i = 0; i < n; i++) {
        for (int j = 0; j < n; j++) {
          result[i][j] = dis.readInt();
        }
      }
      return result;
    } catch (Exception e) {
      return null;
    }
  }

  public static void writeMatrix(int[][] matrix, String path, String fileName) {
    try {      
      File mFile = new File(path + File.separator + fileName);      
      DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(mFile)));
      dos.writeInt(matrix.length * matrix.length);
      for (int i = 0; i < matrix.length; i++) {
        for (int j = 0; j < matrix.length; j++) {
          dos.writeInt(matrix[i][j]);
        }
      }
      dos.close();
    } catch (IOException ex) {
      System.out.println(ex);
    }
  }

  public static int[][] readMatrixS(String path, String fileName) {
    try {     
      Scanner sc = new Scanner(new File(path + File.separator + fileName));
      int nSq = sc.nextInt();
      int n = (int) Math.round(Math.sqrt(nSq));
      int[][] result = new int[n][n];
      for (int i = 0; i < n; i++) {
        for (int j = 0; j < n; j++) {
          result[i][j] = sc.nextInt();
        }
      }
      return result;
    } catch (Exception e) {
      return null;
    }
  }

  public static void writeMatrixS(int[][] matrix, String path, String fileName) {
    try {
      File mFile = new File(path + File.separator + fileName);
      PrintWriter pw = new PrintWriter(mFile);
      pw.printf("%d\n", matrix.length * matrix.length);
      for (int i = 0; i < matrix.length; i++) {
        for (int j = 0; j < matrix.length; j++) {
          pw.printf("%d ", matrix[i][j]);
        }
        pw.println();
      }
      pw.close();
    } catch (FileNotFoundException ex) {
      System.out.println(ex);
    }
  }
  
  /*  Method creates a path "_random" in a given path and returns a name of the 
      newly created path. If the path already exists, only its name is returned. 
   */
  public static String createPathForRandomMatrices(String path) {
    try {
      File fPath = new File(path, "_random");
      if (!fPath.exists())         
        fPath.mkdirs();
      return fPath.getPath();        
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Creates a random matrix of dimension n*n. The values in the matrix are
   * within -maxValue and MaxValue.
   *
   * @param n
   * @param id
   */
  public static int[][] createRNDMatrix(int n, long seed) {
    if (seed == -1) {
      seed = System.nanoTime();
    }

    ARandom rnd = new ARandom(seed);
    int[][] matrix = new int[n][n];

    for (int i = 0; i < n; i++) {
      for (int j = 0; j < n; j++) {
        matrix[i][j] = rnd.nextInt(2 * maxValue + 2) - maxValue;
      }
    }
    return matrix;
  }

  public static int[][] multiply(int[][] A, int[][] B) {
    int[][] C = new int[A.length][A.length];

    for (int i = 0; i < A.length; i++) {
      for (int j = 0; j < A.length; j++) {
        for (int k = 0; k < A.length; k++) {
          C[i][j] += A[i][k] * B[k][j];
        }
      }
    }

    return C;
  }
  
  
  /**
   * Metoda preveri, ali je C res produkt matrik A in B. Za preverjanje uporabi 
   * Freivalds verjetnostni algoritem. Če algoritem vrne false, potem produkt ni
   * pravilen, če pa vrne true, potem je pravilen vsaj z verjetnostno 1/2^k
   */
  public static boolean checkCorrectness(int[][] A, int[][] B, int[][] C, int k) {
    Random rnd = new Random(System.currentTimeMillis());

    for (int p = 0; p < k; p++) {
      int n = A.length; 
      int x[]   = new int[n];    // nakljucni vektor
      int bX[]  = new int[n],    // Bx
          abX[] = new int[n],    // A(Bx)
          cX[]  = new int[n];    // Cx
      
      // ustvarim naključni vektor
      for (int r = 0; r < n; r++) {x[r]=rnd.nextInt(100) < 50 ? 0 : 1;}
      
      // izračunam Bx in Cx ...
      for (int i = 0; i < n; i++) {
        for (int j = 0; j < n; j++) {
          bX[i] += B[i][j]*x[j];
          cX[i] += C[i][j]*x[j];
        }
      }
      // ... ter A(Bx) ...
      for (int i = 0; i < n; i++) {
        for (int j = 0; j < n; j++) {
          abX[i] += A[i][j]*bX[j];
        }
      }
      
      // ...ter primerjam abX in cX po komponentah
      for (int i = 0; i < n; i++) {
        if (cX[i] != abX[i]) return false;
      }            
    }
    return true;
  }

}
