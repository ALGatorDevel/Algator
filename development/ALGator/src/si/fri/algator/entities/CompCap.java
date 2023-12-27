package si.fri.algator.entities;

/**
 *
 * @author tomaz
 */
public enum CompCap {
  UNKNOWN, EM, CNT, JVM, WEB, QUICK;

  @Override
  public String toString() {
    switch (this) {
      case EM:
        return "em";
      case CNT:
        return "cnt";
      case JVM:
        return "jvm";
      case QUICK:
        return "quick";                
      case WEB:
        return "web";
      default:
        return "unknown";
    }
  }

  /**
   * Returns a capability for a given string description
   */
  public static CompCap capability(String cpbDesc) {
    cpbDesc = cpbDesc.toLowerCase(); 
    
    for (CompCap cpb : CompCap.values()) {
      if (cpbDesc.equals(cpb.toString().toLowerCase())) {
        return cpb;
      }
    }
    return UNKNOWN;
  }
}
