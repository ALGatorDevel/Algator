package si.fri.algator.tools;

import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Synchronized ArrayList with elements sorted according to their compareTO method. 
 * To update element's position, call touch(element).
 * @author tomaz
 */
public class SortedArray<E extends Comparable> implements Iterable<E>{
  private CopyOnWriteArrayList data; 

  public SortedArray() {
    data = new CopyOnWriteArrayList<E> ();
  }
  
  public void add(Object element) {
    int placeToInsert=0;
    for(;placeToInsert<data.size(); placeToInsert++ )
      if (((E)element).compareTo(data.get(placeToInsert)) > 0) break;
    data.add(placeToInsert, element);
  }
  
  public void touch(Object element) {
    remove((E)element);
    add(element);
  }
  
  public Iterator<E> iterator() {
    return data.iterator();
  }
  
  public void remove(E element) {
    data.remove(element);
  }
  
  public int size() {
    return data.size();
  }
  
  public E get(int i) {
    return (E) data.get(i);
  }
}
