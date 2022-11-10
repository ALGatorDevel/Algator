/**
 *
 * @author tomaz
 */
public class QuickSortAlgorithm extends BasicSortAbsAlgorithm {

  @Override
  protected BasicSortOutput execute(BasicSortInput testCase) {
    BasicSortOutput result = new BasicSortOutput();

    quickSort(testCase.arrayToSort, 0, testCase.arrayToSort.length-1);    
    result.sortedArray = testCase.arrayToSort;
    
    return result;
  }

  void quickSort(int[] arr, int left, int right) {
    //@COUNT{CALL, 1}

    int i = left, j = right, tmp;

    //@COUNT{R, 1}
    int pivot = arr[(left + right) / 2];

    /* partition */
    while (i <= j) {
    	
      //@COUNT{CMP, 1}
      //@COUNT{R, 1}
      while (arr[i] < pivot) {
        //@COUNT{R, 1}
        //@COUNT{CMP, 1}
        i++;
      }

      //@COUNT{CMP, 1}
      //@COUNT{R, 1}
      while (arr[j] > pivot) {
        //@COUNT{R, 1}
        //@COUNT{CMP, 1}
        j--;
      }

      if (i <= j) {
        //@COUNT{SWAP, 1}
        //@COUNT{R, 2}
        //@COUNT{W, 2}
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
