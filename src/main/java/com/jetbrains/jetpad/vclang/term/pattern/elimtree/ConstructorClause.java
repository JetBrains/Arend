package com.jetbrains.jetpad.vclang.term.pattern.elimtree;

import com.jetbrains.jetpad.vclang.term.definition.Constructor;

public class ConstructorClause<T> {
  private ElimTreeNode<T> myChild;
  private final BranchElimTreeNode<T> myParent;
  private final Constructor myConstructor;

  public ConstructorClause(Constructor constructor, ElimTreeNode<T> child, BranchElimTreeNode<T> parent) {
    setChild(child);
    myParent = parent;
    myConstructor = constructor;
  }

  public BranchElimTreeNode<T> getParent() {
    return myParent;
  }

  void setChild(ElimTreeNode<T> child) {
    myChild = child;
    child.setParent(this);
  }

  public ElimTreeNode<T> getChild() {
    return myChild;
  }

  public Constructor getConstructor() {
    return myConstructor;
  }
}
