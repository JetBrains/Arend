package com.jetbrains.jetpad.vclang.term.pattern.elimtree;

import com.jetbrains.jetpad.vclang.term.definition.Constructor;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.visitor.ElimTreeNodeVisitor;

import java.util.*;

public class BranchElimTreeNode<T> extends ElimTreeNode<T> {
  private final int myIndex;
  private final Map<Constructor, ConstructorClause<T>> myClauses = new HashMap<>();

  public BranchElimTreeNode(int index) {
    myIndex = index;
  }

  @Override
  public <R, P> R accept(ElimTreeNodeVisitor<T, R, P> visitor, P params) {
    return visitor.visitBranch(this, params);
  }

  public int getIndex() {
    return myIndex;
  }

  public void addClause(Constructor constructor, ElimTreeNode<T> node) {
    myClauses.put(constructor, new ConstructorClause<>(constructor, node, this));
  }

  public ElimTreeNode<T> getChild(Constructor constructor) {
    return myClauses.containsKey(constructor) ? myClauses.get(constructor).getChild() : null;
  }

  public Collection<ConstructorClause<T>> getConstructorClauses() {
    return myClauses.values();
  }
}
