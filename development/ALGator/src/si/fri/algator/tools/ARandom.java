package si.fri.algator.tools;

import java.util.concurrent.atomic.AtomicLong;

/**
 * A consistent (java version independent) random generation. The code off this
 * class was copied from java.util.Random (Java 8.0)
 *
 * @author tomaz
 */
public class ARandom {

  private final AtomicLong seed;

  private static final long multiplier = 0x5DEECE66DL;
  private static final long addend = 0xBL;
  private static final long mask = (1L << 48) - 1;

  private static final AtomicLong seedUniquifier = new AtomicLong(8682522807148012L);

  public ARandom() {
    this(seedUniquifier() ^ System.nanoTime());
  }

  public ARandom(long seed) {
    if (getClass() == ARandom.class) {
      this.seed = new AtomicLong(initialScramble(seed));
    } else {
      // subclass might have overriden setSeed
      this.seed = new AtomicLong();
      setSeed(seed);
    }
  }

  private static long seedUniquifier() {
    for (;;) {
      long current = seedUniquifier.get();
      long next = current * 181783497276652981L;
      if (seedUniquifier.compareAndSet(current, next)) {
        return next;
      }
    }
  }

  synchronized public void setSeed(long seed) {
    this.seed.set(initialScramble(seed));
  }

  private static long initialScramble(long seed) {
    return (seed ^ multiplier) & mask;
  }

  protected int next(int bits) {
    long oldseed, nextseed;
    AtomicLong seed = this.seed;
    do {
      oldseed = seed.get();
      nextseed = (oldseed * multiplier + addend) & mask;
    } while (!seed.compareAndSet(oldseed, nextseed));
    return (int) (nextseed >>> (48 - bits));
  }

  public int nextInt(int bound) {
    int r = next(31);
    int m = bound - 1;
    if ((bound & m) == 0) 
      r = (int) ((bound * (long) r) >> 31);
    else 
      for (int u = r;  u - (r = u % bound) + m < 0; u = next(31));    
    return r;
  }

  public long nextLong() {
    return ((long) (next(32)) << 32) + next(32);
  }
}
