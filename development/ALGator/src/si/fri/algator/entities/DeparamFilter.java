package si.fri.algator.entities;

/**
 * Deparameterized filter (i.e. $1 is replaced with paramValue)
 * @author tomaz
 */
public class DeparamFilter {
  private Number paramValue;
  private String [] filter;

  public DeparamFilter(Number paramValue, String [] filter) {
    this.paramValue = paramValue;
    this.filter = filter;
  }

  public Number getParamValue() {
    return paramValue;
  }

  public void setParamValue(Number paramValue) {
    this.paramValue = paramValue;
  }

  public String [] getFilter() {
    return filter;
  }

  public void setFilter(String [] filter) {
    this.filter = filter;
  }
  
  
}
