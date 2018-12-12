package si.fri.algotest.analysis;

import static java.lang.System.currentTimeMillis;

/**
 *
 * @author ernest, judita
 */
public class CacheEntry {

    private Object value;

    private long timestamp;

    public Object getValue() {
        timestamp = currentTimeMillis();
        return value;
    }

    public long getTime() {
        return timestamp;
    }

    public CacheEntry(Object val) {
        value=val;
        timestamp = currentTimeMillis();
    }

}
