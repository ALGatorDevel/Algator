package si.fri.algator.analysis.complexity;

import java.math.BigInteger;
import java.util.ArrayList;
import org.apache.commons.math3.fitting.WeightedObservedPoint;

/**
 *
 * @author tomaz
 */
public class DataTools {

  /**
   * Converts x[], y[] to an Arraylist of WeightedObservedPoint (with weights
   * set to default value 1).
   */
  public static ArrayList<WeightedObservedPoint> getObservedPoints(double[] x, double[] y) {
    ArrayList<WeightedObservedPoint> result = new ArrayList();
    for (int i = 0; i < x.length; i++) {
      result.add(new WeightedObservedPoint(1, x[i], y[i]));
    }
    return result;
  }

  /**
   * Sums the squares of the diferences between y-values and the values of f 
   * in corresponding x (f(x[i]) - y[i]). 
   * Result: sqrt(Sum_i((f(x[i]) - y[i])^2 ) / n)
   */
  public static double rmse(double[] x, double[] y, FittingFunction f, double... params) {
    double result = 0;
    int n = x.length;
    for (int i = 0; i < n; i++) {
      double y1 = y[i];
      double y2 = f.value(x[i], params);
      result += Math.pow(y[i] - f.value(x[i], params), 2);
    }
    return Math.sqrt(result/n);
  }
  
  /**
   * ROOT MEAN SQUARE PERCENTAGE ERROR
   * 
   * Result: sqrt(Sum_i(((f(x[i]) - y[i])/f(x[i]))^2 ) / n)
   * 
   * see: A survey of forecast Error Measures
   * see: ALGator bookmarks (End-to-end ....)
   */
  public static double rmspe(double[] x, double[] y, FittingFunction f, double... params) {
    double result = 0;
    for (int i = 0; i < x.length; i++) {
      double y1 = y[i];                   // prediction
      double y2 = f.value(x[i], params);  // actual value of function
      
      result += Math.pow( (y1-y2)/y2 , 2);
    }
    return Math.sqrt(result/x.length);
  }
  
  
  

}
