package si.fri.algator.analysis.complexity;

/**
 *
 * @author tomaz
 */
public enum FunctionType {
  CONST,
  LOGLOG,
  LOG,
  LINEAR,
  LINEARITHMIC,
  POLY2,
  POLY3,
  EXP,   
  CLOGLOG,
  CLOG,
  CLINEAR,
  CLINEARITHMIC,
  CPOLY2,
  CPOLY3;

  public int numberOfParams() {
    switch (this) {
      case CONST:
      case CLOG:
      case CLOGLOG:
      case CLINEAR:
      case CLINEARITHMIC:
      case CPOLY2:
      case CPOLY3:
        return 1;
      case LOGLOG:
      case LOG:
      case LINEAR:
      case LINEARITHMIC:
      case EXP:
        return 2;
      case POLY2:
        return 3;
      case POLY3:
        return 4;
      default:
        return 0;
    }
  }
  
  public String getSymbolic() {
    switch (this) {
      case CONST:
        return "1";
      case LOGLOG:
      case CLOGLOG:
        return "log(x)*log(x)";
      case LOG:
      case CLOG:
        return "log(x)";
      case LINEAR:
      case CLINEAR:
        return "x";
      case LINEARITHMIC:
      case CLINEARITHMIC:
        return "x*log(x)";
      case EXP:
        return "exp(x)";
      case POLY2:
      case CPOLY2:
        return "x^2";
      case POLY3:
      case CPOLY3:
        return "x^3";
      default:
        return "?";
    }
  }  

  public String toString(double... params) {
    switch (this) {
      case CONST:
      case LINEAR:
      case POLY2:
      case POLY3:
        String result = "";
        for (int i = 0; i < params.length; i++) {
          result += String.format("%s %.10f%s", 
            (i == 0 ? "" : (params[1] < 0 ? " - ":" + ")), Math.abs(params[i]), (i == 0 ? "" : "x^" + i));
        }
        return result;
      case LOG: case LOGLOG: case LINEARITHMIC: case EXP:
        return String.format("%.10f %s %.10f*%s", 
          params[0], (params[1] < 0 ? "-":"+"), Math.abs(params[1]), this.getSymbolic());
      case CLINEAR:
      case CLINEARITHMIC:
      case CLOG:
      case CLOGLOG:
      case CPOLY2:
      case CPOLY3:
        return String.format("%.10f%s", params[0], this.getSymbolic());
      default:
        return "?";
    }

  }
}
