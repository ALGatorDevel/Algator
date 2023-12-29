package basicsort;

/**
  * This class contains helper methods that will be used 
  * to check the correctness of the algorithms' results 
  * and to present the results in a "human-readable" way. 
  */ 
public class Tools {

  /**
   * Returns true is an aray is sorted
   * @param order 1 ... increasing order, -1 ... decreasing order
   */
  public static boolean isArraySorted(int tab[], int order) {
    for (int i = 0; i < tab.length-1; i++) {
      if (order * tab[i] > order * tab[i+1]) return false;
    }
    return true;
  }



  /**
   * Returns a string representation of an array (first 10 elements)
   */
  public static <E> String arrarToString(E [] array) {
    if (array == null) return "null";
    
    int i;
    String result = "";
    for (i = 0; i < Math.min(10, array.length); i++) {
      if (i>0) result+=",";
      result += array[i].toString();
    }
    if (i<array.length)
      result += ", ... (" + array.length + " elements)";
    return "[" + result + "]";
  }


  /**
   * Method returns a string representation of an array
   */
  public static String intArrayToString(int [] array) {
    if (array == null) return "null"; 
	    
    Integer [] tab = new Integer[array.length];
    for(int i=0; i<tab.length; i++)
      tab[i] = array[i];
    return arrarToString(tab);
  }   


}
