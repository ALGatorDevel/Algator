package si.fri.algator.entities;

/**
 *
 * @author tomaz
 */
public enum AlgorithmLanguage {
  UNKNOWN, JAVA, C;

  @Override
  public String toString() {
    switch(this) {
      case JAVA:
        return "java";
      case C:
        return "c";
      default:
        return "/unknown/";
    }
  }
        
  static AlgorithmLanguage getType(String typeDesc) {
    for (AlgorithmLanguage type : AlgorithmLanguage.values())
      if (typeDesc.equalsIgnoreCase(type.toString())) 
	return type;
    return UNKNOWN;
  }
}
