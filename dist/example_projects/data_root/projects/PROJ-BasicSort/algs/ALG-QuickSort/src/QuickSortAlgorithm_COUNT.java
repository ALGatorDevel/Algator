import si.fri.algotest.execute.Counters;
/**
 *
 * @author tomaz
 */
public class QuickSortAlgorithm_COUNT extends BasicSortAbsAlgorithm {

  @Override
  protected BasicSortOutput execute(BasicSortInput testCase) {
    BasicSortOutput result = new BasicSortOutput();

    quickSort(testCase.arrayToSort, 0, testCase.arrayToSort.length-1);    
    result.sortedArray = testCase.arrayToSort;
    
    return result;
  }

  void quickSort(int[] arr, int left, int right) {
    Counters.addToCounter("CALL",  1);

    int i = left, j = right, tmp;

    Counters.addToCounter("R",  1);
    int pivot = arr[(left + right) / 2];

    /* partition */
    while (i <= j) {
    	
      Counters.addToCounter("CMP",  1);
      Counters.addToCounter("R",  1);
      while (arr[i] < pivot) {
        Counters.addToCounter("R",  1);
        Counters.addToCounter("CMP",  1);
        i++;
      }

      Counters.addToCounter("CMP",  1);
      Counters.addToCounter("R",  1);
      while (arr[j] > pivot) {
        Counters.addToCounter("R",  1);
        Counters.addToCounter("CMP",  1);
        j--;
      }

      if (i <= j) {
        Counters.addToCounter("SWAP",  1);
        Counters.addToCounter("R",  2);
        Counters.addToCounter("W",  2);
        tmp = arr[i];
        arr[i] = arr[j];
        arr[j] = tmp;
        i++;
        j--;
      }

    };

    /* recursion */
    if (left < j) 
      quickSort(arr, left, j);
   
    if (i < right) 
      quickSort(arr, i, right);
  }
}

