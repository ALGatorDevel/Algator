package si.fri.algator.tools;

/**
 *
 * @author tomaz
 */
public class DT {

  static long start = 0;
  
  public static void start() {
    start = System.currentTimeMillis();
  }
  public static void print(String msg) {
    long time = (System.currentTimeMillis() - start);
    System.out.printf("Time: %10d (%s)\n", time, msg);
  }
  
}
