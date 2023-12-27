package si.fri.algator.execute;

import java.util.HashMap;

/**
 *
 * @author tomaz
 */
public class Counters {

  private static HashMap<String, Integer> counters = new HashMap();

  public static HashMap<String, Integer> getCounters() {
    return counters;
  }
  
  public static void resetCounters() {
    counters = new HashMap();
  }
  
  public static int getCounterValue(String counter) {
    if (counters.containsKey(counter))
      return counters.get(counter);
    else
      return 0;
  }
  
  public static void addToCounter(String counter, int add) {
    int value = 0;
    if (counters.containsKey(counter))
      value = counters.get(counter);
    value += add;
    
    counters.put(counter, value);
  }
  
}
