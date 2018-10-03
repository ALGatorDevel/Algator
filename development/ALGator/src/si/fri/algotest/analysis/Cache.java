package si.fri.algotest.analysis;

import static java.lang.System.currentTimeMillis;
import java.util.HashMap;
import java.util.Map.Entry;

/**
 *
 * @author ernest, judita
 */
public class Cache {

    private static HashMap<Object, CacheEntry> cache;

    private static HashMap<Object, CacheEntry> getCache() {
        if (cache == null) {
            cache = new HashMap<>();
        }
        return cache;
    }

    public static void set(Object key, Object value) {
        getCache().put(key, new CacheEntry(value));
        long ct = currentTimeMillis();
        for (Entry entry : getCache().entrySet()) {
            if (((CacheEntry) entry.getValue()).getTime() + 5 * 60 * 1000 < ct) {
                getCache().remove(entry);
            }
        }
    }

    public static Object get(String key) {
        CacheEntry ce = getCache().get(key);
        if (ce != null) {
            return ce.getValue();
        } else {
            return null;
        }
    }

}
