package si.fri.algotest.entities;

/**
 *
 * @author tomaz
 */
public enum ComputerCapability {
  AEE_UNKNOWN, AEE_EM, AEE_CNT, AEE_JVM, AEE_WEB, AEE_QUICK;

  @Override
  public String toString() {
    switch (this) {
      case AEE_EM:
        return "aee_em";
      case AEE_CNT:
        return "aee_cnt";
      case AEE_JVM:
        return "aee_jvm";
      case AEE_QUICK:
        return "aee_quick";                
      case AEE_WEB:
        return "aee_web";
      default:
        return "aee_unknown";
    }
  }

  /**
   * Returns a capability for a given string description; valid descriptions are 
   * aee_em, aee_jvm, ..., as well as em, jvm, ...
   */
  public static ComputerCapability getComputerCapability(String cpbDesc) {
    cpbDesc = cpbDesc.toLowerCase(); 
    if (!cpbDesc.startsWith("aee_")) cpbDesc = "aee_" + cpbDesc;
    
    for (ComputerCapability cpb : ComputerCapability.values()) {
      if (cpbDesc.equals(cpb.toString().toLowerCase())) {
        return cpb;
      }
    }
    return AEE_UNKNOWN;
  }
}
