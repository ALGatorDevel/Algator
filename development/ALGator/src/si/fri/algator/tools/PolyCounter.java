package si.fri.algator.tools;

/**
 * Večparametrski števec. Vsaka "cifra" v tem števcu ima lahko različno maksimalno 
 * vrednost; pri povečevanju tega števca, se najprej poskusi povečati zadnjo cifro; 
 * če ima ta že maksimalno vrednost, se postavi na 0 in poskusi povečati predzadnjo
 * cifro; če imajo vse cifre že maksimalno vrednost, števec vrne null.
 * @author tomaz
 */
public class PolyCounter {
  private int [] maxValue;     // max values for each digit
  private int [] currentValue; // the current value of the counter
  private boolean overflow;    // true when increased over its max value
  
  public PolyCounter(int [] maxValue) {
    this.maxValue     = maxValue;
    this.currentValue = new int[this.maxValue.length];
    this.overflow     = false;
  }      
  
  public boolean overflow() {
    return overflow;
  }

  public int[] getValue() {
    return currentValue;
  }

  
  public void nextValue() {
    overflow = false;
    for (int i = maxValue.length-1; i >=0 ; i--) {
      if (currentValue[i] < maxValue[i]) {
        currentValue[i]++;
        return;
      } else {
        currentValue[i]=0;
      }         
    }
    overflow = true;
  }
  
  public int getNumberOfValues() {
    int result = 1;
    for (int i = 0; i < maxValue.length; i++) 
      result *= (maxValue[i]+1);  
    return result;
  }
    
  public static void main(String[] args) {
    //PolyCounter pc = new PolyCounter(new int[] {3});
    PolyCounter pc = new PolyCounter(new int[] {3,2});
    //PolyCounter pc = new PolyCounter(new int[] {3, 0, 2});
    while (!pc.overflow()) {
      int [] value = pc.getValue();
      for (int i : value) {
        System.out.print(i + " ");
      }
      System.out.println("");
            pc.nextValue();
    }
  }
}
