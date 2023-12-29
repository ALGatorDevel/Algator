package si.fri.algator.entities;

/**
 * A type of a result variable.
 *
 * @author tomaz
 */
public enum VariableType {

  UNKNOWN, TIMER, COUNTER, INT, DOUBLE, STRING, ENUM, JSONSTRING;

  @Override
  public String toString() {
    switch (this) {
      case UNKNOWN:
        return "unknown";
      case TIMER:
        return "timer";
      case COUNTER:
        return "counter";
      case INT:
        return "int";
      case DOUBLE:
        return "double";
      case STRING:
        return "string";        
      case ENUM:
        return "enum";
      case JSONSTRING:
        return "jsonstring";                      
      default:
        return "/unknown/";
    }
  }

  static VariableType getType(String typeDesc) {
    for (VariableType rst : VariableType.values()) {
      if (typeDesc.equals(rst.toString())) {
        return rst;
      }
    }
    return UNKNOWN;
  }

  /**
   * The default value for parameters of this type
   */
  public Object getDefaultValue() {
    switch (this) {
      case TIMER:
      case COUNTER:
      case INT:
        return 0;
      case DOUBLE:
        return 0.0;
      case STRING:
        return "";
      case ENUM:  
        return "";
    }
    return null;
  }

}
