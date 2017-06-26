package com.jetbrains.jetpad.vclang.core.pattern.elimtree;

import com.jetbrains.jetpad.vclang.core.context.binding.Binding;
import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.core.pattern.elimtree.visitor.ElimTreeNodeVisitor;
import com.jetbrains.jetpad.vclang.core.subst.ExprSubstitution;

import java.util.List;

public class EmptyElimTreeNode extends ElimTreeNode {
  private static final EmptyElimTreeNode myInstance = new EmptyElimTreeNode();
  private EmptyElimTreeNode() {}

  public static EmptyElimTreeNode getInstance() {
    return myInstance;
  }

  @Override
  public <P, R> R accept(ElimTreeNodeVisitor<P, R> visitor, P params) {
    return visitor.visitEmpty(this, params);
  }

  @Override
  public ElimTreeNode matchUntilStuck(ExprSubstitution subst, boolean normalize) {
    return this;
  }

  @Override
  public void updateLeavesMatched(List<Binding> bindings) {

  }

  @Override
  public LeafElimTreeNode match(List<Expression> expressions) {
    return null;
  }

  @Override
  public String toString() {
    return "⊥";
  }
}
