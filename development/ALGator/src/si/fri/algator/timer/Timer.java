package si.fri.algator.timer;

import java.io.Serializable;

/**
 * Time of execution in micro (10^(-6)) seconds
 * @author tomaz
 */
public class Timer implements Serializable {

  // maximu number of timers supported by ALGator
  public static final int MAX_TIMERS = 5;

  // current timer from 0, ..., MAX_TIMERS-1
  private int curTimer;

  private final long[] startTime, stopTime;

  public Timer() {
    curTimer = 0;

    startTime = new long[MAX_TIMERS];
    stopTime = new long[MAX_TIMERS];
  }

  /**
   * Starts the i-th timer
   */
  public void start(int i) {
    startTime[i] = System.nanoTime();
  }

  /**
   * Starts the current timer
   */
  public void start() {
    start(curTimer);
  }

  /**
   * Stops the i-th timer
   */
  public void stop(int i) {
    stopTime[i] = System.nanoTime();
  }

  /**
   * Stops the current timer
   */
  public void stop() {
    stop(curTimer);
  }

  /**
   * Resumes the i-th timer
   */
  public void resume(int i) {
    startTime[i] = System.nanoTime()- (stopTime[i] - startTime[i]);
  }

  /**
   * Resumes the current timer
   */
  public void resume() {
    resume(curTimer);
  }
  
  /**
   * This method performs three tasks: 1) stops the current timer, 
   * 2) set the current timer to be current timer + 1
   * 3) starts (new) current timer
   */
  public void next() {
    curTimer++;
    if (curTimer >= MAX_TIMERS) {
      throw new RuntimeException("Maximum number of timers exceeded.");
    }

    start();
  }

  public long time(int i) {
    return (stopTime[i] - startTime[i]) / 1000;
  }

  public long time() {
    return time(curTimer);
  }
}
