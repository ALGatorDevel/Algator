package si.fri.algator.execute;

/**
 *
 * @author tomaz
 */
abstract public class AbstractIndicatorTest<T,O> {
  public abstract Object getValue(T testCase, O algorithmOutput);
}
