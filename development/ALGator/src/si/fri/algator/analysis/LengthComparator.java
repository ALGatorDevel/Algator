package si.fri.algator.analysis;

import java.util.Comparator;

/**
 *
 * @author Ernest
 */
public class LengthComparator implements Comparator<String> {

    public int compare(String o1, String o2) {
        return Integer.compare(o1.length(), o2.length());
    }
}
