package si.fri.algator.analysis.complexity;

import org.apache.commons.math3.analysis.ParametricUnivariateFunction;

public class FittingFunction implements ParametricUnivariateFunction {

  FunctionType type;

  public FittingFunction(FunctionType type, double... params) {
    this.type = type;
  }


  @Override
  public double value(double x, double... params) {
    switch (this.type) {
      case CONST:
        return params[0];
      case LOGLOG:
        return params[0] + params[1] * Math.log(x) * Math.log(x);
      case LOG:
        return params[0] + params[1] * Math.log(x);
      case LINEAR:
        return params[0] + params[1] * x;
      case LINEARITHMIC:
        return params[0] + params[1] * x * Math.log(x);
      case POLY2:
      case POLY3:
        double result = 0;
        double pX = 1;
        for (int i = 0; i < params.length; i++) {
          result += params[i] * pX;
          pX *= x;
        }
        return result;
      case EXP:
        return params[0] + params[1] * Math.exp(x);
      case CLOGLOG:
        return params[0]*Math.log(x)*Math.log(x);
      case CLOG:
        return params[0]*Math.log(x);
      case CLINEAR:
        return params[0]*x;
      case CLINEARITHMIC:
        return params[0]*x*Math.log(x);
      case CPOLY2:
        return params[0]*x*x;
      case CPOLY3:
        return params[0]*x*x*x;
      default:
        return 0;
    }
  }

  @Override
  public double[] gradient(double x, double... params) {
    switch (this.type) {
      case CONST:
        return new double[]{1};
      case LOGLOG:
        return new double[]{1, Math.log(x) * Math.log(x)};
      case LOG:
        return new double[]{1, Math.log(x)};
      case LINEAR:
        return new double[]{1, x};
      case LINEARITHMIC:
        return new double[]{1, x * Math.log(x)};
      case POLY2:
      case POLY3:
        double[] result = new double[params.length];
        double pX = 1;
        for (int i = 0; i < params.length; i++) {
          result[i] = pX;
          pX *= x;
        }
        return result;
      case EXP:
        return new double[]{1, Math.exp(x)};
      case CLOGLOG:
        return new double[]{Math.log(x)*Math.log(x)};
      case CLOG:
        return new double[]{Math.log(x)};
      case CLINEAR:
        return new double[]{x};
      case CLINEARITHMIC:
        return new double[]{x*Math.log(x)};
      case CPOLY2:
        return new double[]{x*x};
      case CPOLY3:
        return new double[]{x*x*x};
      default:
        return new double[]{0};
    }
  }  

  @Override
  public String toString() {
    return this.type.toString();
  }
  
  
}
