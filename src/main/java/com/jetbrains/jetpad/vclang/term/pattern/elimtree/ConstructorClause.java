package com.jetbrains.jetpad.vclang.term.pattern.elimtree;

import com.jetbrains.jetpad.vclang.term.definition.Constructor;

public class ConstructorClause {
  private ElimTreeNode myChild;
  private final BranchElimTreeNode myParent;
  private final Constructor myConstructor;

  public ConstructorClause(Constructor constructor, ElimTreeNode child, BranchElimTreeNode parent) {
    setChild(child);
    myParent = parent;
    myConstructor = constructor;
  }

  public BranchElimTreeNode getParent() {
    return myParent;
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
