package com.jetbrains.jetpad.vclang.term.pattern.elimtree;

import com.jetbrains.jetpad.vclang.term.pattern.elimtree.visitor.ElimTreeNodeVisitor;

public abstract class ElimTreeNode<T> {
  private ConstructorClause<T> myParent = null;

  public ConstructorClause<T> getParent() {
    return myParent;
  }

  void setParent(ConstructorClause<T> parent) {
    myParent = parent;
  }

  public abstract <R, P> R accept(ElimTreeNodeVisitor<T, R, P> visitor, P params);
}
