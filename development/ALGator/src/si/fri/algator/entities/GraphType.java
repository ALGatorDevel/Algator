package si.fri.algator.entities;

/**
 *
 * @author tomaz
 */
public enum GraphType {

  LINE, STAIR, BAR, BOX, PIE, AREA, DONUT, UNKNOWN; 

  @Override
  public String toString() {
    switch(this) {
      case LINE:
        return "line";
      case STAIR:
        return "stair";
      case BAR:
        return "bar";
      case BOX:
        return "box";
      case PIE:
        return "pie";
      case AREA: 
        return "area";
      case DONUT:
        return "donut";
      default:
        return "/unknown/";
    }
  }
  
  
    
  static GraphType getType(String typeDesc) {
    for (GraphType type : GraphType.values())
      if (typeDesc.equalsIgnoreCase(type.toString())) 
	return type;
    return UNKNOWN;
  }
}
