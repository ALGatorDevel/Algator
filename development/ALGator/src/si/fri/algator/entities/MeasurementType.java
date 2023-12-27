package si.fri.algator.entities;

/**
 *
 * @author tomaz
 */
public enum MeasurementType  {
  UNKNOWN, EM, JVM, CNT;

  @Override
  public String toString() {
    switch (this) {
      case EM:
        return "Regular mesurement of parameters and timers"; 
      case JVM:
        return "Measurement of JVM parameters";
      case CNT:
        return "Measurement of user defined counters";
      default:
        return "/unknown/";
    }
  }
  
  static public MeasurementType mtOf(String mt) {
    if (mt.equalsIgnoreCase("cnt")) return MeasurementType.CNT;
    if (mt.equalsIgnoreCase("jvm")) return MeasurementType.JVM;
    return MeasurementType.EM;
  }

  /**
   * Returns the extension to the result file for a  measurement of a given type
   * (results/AlgName-TestSetname.extension)
   */
  public String getExtension() {
    switch (this) {
      case EM:  return "em";
      case JVM: return "jvm";
      case CNT: return "cnt";	
    }
    return "";
  }
}
