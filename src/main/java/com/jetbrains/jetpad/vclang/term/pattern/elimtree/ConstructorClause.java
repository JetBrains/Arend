package com.jetbrains.jetpad.vclang.term.pattern.elimtree;

import com.jetbrains.jetpad.vclang.term.definition.Constructor;

public class ConstructorClause {
  private ElimTreeNode myChild;
  private final Constructor myConstructor;

  public ConstructorClause(Constructor constructor, ElimTreeNode child) {
    setChild(child);
    myConstructor = constructor;
  }

  void setChild(ElimTreeNode child) {
    myChild = child;
    child.setParent(this);
  }

  public ElimTreeNode getChild() {
    return myChild;
  }

  public Constructor getConstructor() {
    return myConstructor;
  }
}
