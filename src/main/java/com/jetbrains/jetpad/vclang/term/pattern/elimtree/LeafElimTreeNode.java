package com.jetbrains.jetpad.vclang.term.pattern.elimtree;

import com.jetbrains.jetpad.vclang.term.pattern.elimtree.visitor.ElimTreeNodeVisitor;

public class LeafElimTreeNode<T> extends ElimTreeNode<T> {
  private T myValue;

  public LeafElimTreeNode(T value) {
    myValue = value;
  }

  public T getValue() {
    return myValue;
  }

  void setValue(T value) {
    myValue = value;
  }

  public ElimTreeNode<T> replaceWith(ElimTreeNode<T> root, ElimTreeNode<T> replacement) {
    if (getParent() == null) {
      return replacement;
    }
    getParent().setChild(replacement);
    return root;
  }

  @Override
  public <R, P> R accept(ElimTreeNodeVisitor<T, R, P> visitor, P params) {
    return visitor.visitLeaf(this, params);
  }
}
