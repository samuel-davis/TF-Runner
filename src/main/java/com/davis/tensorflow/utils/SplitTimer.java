package com.davis.tensorflow.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple utility timer for measuring CPU time and wall-clock splits.
 */
public class SplitTimer {
  private final Logger logger = LoggerFactory.getLogger(SplitTimer.class.getName());

  private long lastWallTime;
  private long lastCpuTime;

  public SplitTimer(final String name) {

    newSplit();
  }

  public void newSplit() {
    lastWallTime = System.currentTimeMillis();
    lastCpuTime = System.currentTimeMillis();
  }

  public void endSplit(final String splitName) {
    final long currWallTime = System.currentTimeMillis();
    final long currCpuTime = System.currentTimeMillis();

    logger.info(
        "{}: cpu={} wall={}",
        splitName, currCpuTime - lastCpuTime, currWallTime - lastWallTime);

    lastWallTime = currWallTime;
    lastCpuTime = currCpuTime;
  }
}

