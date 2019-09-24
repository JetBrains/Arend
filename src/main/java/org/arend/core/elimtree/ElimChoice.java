package org.arend.core.elimtree;

public class ElimChoice {
  public final boolean splitConstructor;
  public final ElimTree elimTree;

  public ElimChoice(boolean splitConstructor, ElimTree elimTree) {
    this.splitConstructor = splitConstructor;
    this.elimTree = elimTree;
  }
}
