/**
 *
 * @author tomaz
 */
public class InsertionSortAlgorithm extends BasicSortAbsAlgorithm {

   @Override
  protected BasicSortOutput execute(BasicSortInput testCase) {
    BasicSortOutput result = new BasicSortOutput();

    execute(testCase.arrayToSort);    
    result.sortedArray = testCase.arrayToSort;
    
    return result;
  }
  
  public void execute(int[] data) {
    for(int i=1; i<data.length; i++) {
      int j=i-1;

      /*//@REMOVE_LINE
      if (j>=0) 
        //@COUNT{CMP, 1}
      *///@REMOVE_LINE
      while(j>=0 && data[j]>data[j+1]) {
      	//@COUNT{CMP, 1}

        //@COUNT{SWAP, 1}
        int tmp = data[j];
        data[j]=data[j+1];
        data[j+1]=tmp;
        j--;
      }
    }
  } 
}