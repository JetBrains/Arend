package com.jetbrains.jetpad.vclang.core.pattern.elimtree;

import com.jetbrains.jetpad.vclang.core.subst.ExprSubstitution;

public class OtherwiseClause implements Clause {
  private ElimTreeNode myChild;
  private final BranchElimTreeNode myParent;

  OtherwiseClause(BranchElimTreeNode parent) {
    myParent = parent;
    setChild(EmptyElimTreeNode.getInstance());
  }

  public BranchElimTreeNode getParent() {
    return myParent;
  }

  @Override
  public void setChild(ElimTreeNode child) {
    myChild = child;
    child.setParent(this);
  }

  @Override
  public ElimTreeNode getChild() {
    return myChild;
  }

  @Override
  public ExprSubstitution getSubst() {
    return new ExprSubstitution();
  }
}
