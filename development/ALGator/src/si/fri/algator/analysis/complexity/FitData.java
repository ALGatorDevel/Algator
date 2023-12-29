package si.fri.algator.analysis.complexity;

import java.util.Arrays;
import org.apache.commons.math3.analysis.ParametricUnivariateFunction;
import org.apache.commons.math3.fitting.SimpleCurveFitter;

/**
 *
 * @author tomaz
 */
public class FitData {
  
  public static double[] findFit(double[] x, double[] y, FunctionType fType) {
    ParametricUnivariateFunction func = new FittingFunction(fType);
    try {
      double[] coef = new double[fType.numberOfParams()]; Arrays.setAll(coef, i-> 1);
      SimpleCurveFitter scf = SimpleCurveFitter.create(func, coef);
      return scf.fit(DataTools.getObservedPoints(x, y));
    } catch (Exception e) {
      return new double[fType.numberOfParams()];
    }
  }
  
  public static FittingFunction findBestFit(double[] x, double[] y) {
    return null;
  }
  
  public static  double[] calculateKoefs(double[] x, double[] y, FunctionType fType) {            
    double[] koef = new double[x.length];
    for (int i = 0; i < x.length; i++) {
      try {
      switch (fType) {
        case CONST:
          koef[i] = y[i];
          break;
          
        case LOG:
          koef[i] = y[i] / Math.log(x[i]);
          break;
          
        case LOGLOG:
          koef[i] = y[i] / (Math.log(x[i]) * Math.log(x[i]));
          break;

        case LINEAR:
          koef[i] = y[i] / x[i];
          break;

        case LINEARITHMIC:
          koef[i] = y[i] / (x[i]*Math.log(x[i]));
          break;

        case POLY2:
          koef[i] = y[i] / (x[i]*x[i]);
          break;

        case POLY3:
          koef[i] = y[i] / (x[i]*x[i]*x[i]);
          break;

        case EXP:
          koef[i] = y[i] / Math.exp(x[i]);
          break;          
      }
      } catch (Exception e) {koef[i] = 0;}
    }
    return koef;
  }
  
  

}
