package si.fri.algator.entities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

/**
 *
 * @author tomaz
 */
public enum StatFunction {

  UNKNOWN, MIN, MAX, AVG, SUM, ALL, FIRST, LAST, MED;

  @Override
  public String toString() {
    switch (this) {
      case UNKNOWN:
        return "unknown";
      case MIN:
        return "MIN";
      case MAX:
        return "MAX";
      case AVG:
        return "AVG";
      case SUM:
        return "SUM";
      case ALL:
        return "ALL";        
      case FIRST:
        return "FIRST";
      case LAST:
        return "LAST";
      case MED:
        return "MED";
      default:
        return "/unknown/";
    }
  }

  public static StatFunction getStatFunction(String desc) {
    for (StatFunction sfnc : StatFunction.values()) {
      if (desc.equals(sfnc.toString())) {
        return sfnc;
      }
    }
    return UNKNOWN;
  }

  /**
   * Calculates the value of given {@code funcion} mapped over {@code vallues}.
   */
//  public static long getFunctionValue(StatFunction function, long[] values) {
//    if (values == null || values.length == 0) {
//      return -1;
//    }
//
//    long val = values[0];
//    switch (function) {
//      case MIN:
//      case MAX:
//        for (int i = 1; i < values.length; i++) {
//          if ((function.equals(MIN) && values[i] < val) || (function.equals(MAX) && values[i] > val)) {
//            val = values[i];
//          }
//        }
//        return val;
//      case SUM:
//      case AVG:
//        for (int i = 1; i < values.length; i++) {
//          val += values[i];
//        }
//        if (function.equals(SUM)) {
//          return val;
//        } else {
//          return val / values.length;
//        }
//      default:
//        return -1;
//    }
//  }
  public static Object getFunctionValue(StatFunction function, ArrayList<? extends Comparable> values) {
    Iterator it = values.iterator();
    while (it.hasNext()) {
      Object next = it.next();
      if (next == null || next.equals("null") || next.equals("?")) 
        it.remove();
    }    
    
    if (values == null || values.size() == 0) {
      return null;
    }

    // For non-numbers only FIRST and LAST are exceptable; FIRST is default.
    if (!(values.get(0) instanceof Number)) {
      if (!function.equals(StatFunction.LAST)) {
        function = StatFunction.FIRST;
      }
    }

    switch (function) {
      case FIRST:
        return values.get(0);
      case LAST:
        return values.get(values.size() - 1);
      case MIN:
      case MAX:
        Comparable val = values.get(0);
        for (int i = 1; i < values.size(); i++) {
          if ((function.equals(MIN) && values.get(i).compareTo(val) < 0) || (function.equals(MAX) && values.get(i).compareTo(val) > 0)) {
            val = values.get(i);
          }
        }
        return val;
      case SUM:
      case AVG:
        try {
          double valN = ((Number) values.get(0)).doubleValue();
          for (int i = 1; i < values.size(); i++) {
            valN += ((Number) values.get(i)).doubleValue();
          }

          if (function.equals(AVG)) {
            valN /= values.size();
          }
          if (values.get(0) instanceof Integer) {
            return (int)valN;
          } else if (values.get(0) instanceof Long) {
            return (long)valN;
          } else {
            return valN;
          }
        } catch (Exception e) {
          // this exception will probably occur only if values are not Numbers
          return null;
        }
      // added by Ziga Zorman
      case MED:
        try {
          double median = ((Number) values.get(0)).doubleValue();
          if (values.size() == 2) {
            median += ((Number) values.get(1)).doubleValue();
            median /= 2;
          } else if (values.size() > 2) {
            ArrayList<? extends Comparable> sortedValues = new ArrayList<>(values);
            Collections.sort(sortedValues);
            int halfSize = sortedValues.size() / 2;
            if (sortedValues.size() % 2 == 0) {
              median = (((Number) sortedValues.get(halfSize - 1)).doubleValue() + ((Number) sortedValues.get(halfSize)).doubleValue()) / 2;
            } else {
              median = ((Number) sortedValues.get(halfSize)).doubleValue();
            }
          }
          if (values.get(0) instanceof Integer) {
            return (int)median;
          } else if (values.get(0) instanceof Long) {
            return (long)median;
          } else {
            return median;
          }
        } catch (Exception e) {
          // this exception will probably occur only if values are not Numbers
          return -1;
        }
      case ALL:
        return values.toString().replaceAll("[\\[\\] ]", "");
        
      default:
        return -1;
    }
  }
  
}
