package com.palantir.javaformat;

/** How to decide where to break when a level can't fit on a single line. */
public enum BreakBehaviour {
  /** Always break this level. */
  BREAK_THIS_LEVEL,
  /**
   * If the last level is breakable, prefer breaking it if it will keep the rest of this level on
   * line line.
   */
  PREFER_BREAKING_LAST_INNER_LEVEL,
}
