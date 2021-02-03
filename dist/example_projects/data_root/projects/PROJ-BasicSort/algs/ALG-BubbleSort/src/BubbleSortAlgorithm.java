/**
 *
 * @author tomaz
 */
public class BubbleSortAlgorithm extends BasicSortAbsAlgorithm {

  @Override
  protected BasicSortOutput execute(BasicSortInput input) {
    BasicSortOutput result = new BasicSortOutput();

    execute(input.arrayToSort);   
    
    result.sortedArray = input.arrayToSort;
    
    return result;
  }

  private void execute(int[] data) {
     for (int i=0; i<data.length; i++)
       for (int j=0; j<data.length-1; j++) {
       	 //@COUNT{CMP, 1}
         if (data[j] > data[j+1]) {
           //@COUNT{SWAP, 1}	
           int tmp = data[j];
           data[j] = data[j+1];
           data[j+1]=tmp;
         }
       }
   }
}