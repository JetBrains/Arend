package org.arend.core.elimtree;

public abstract class ElimTree implements Body {
  protected final int skipped;

  ElimTree(int skipped) {
    this.skipped = skipped;
  }

  public int getSkipped() {
    return skipped;
  }
}
