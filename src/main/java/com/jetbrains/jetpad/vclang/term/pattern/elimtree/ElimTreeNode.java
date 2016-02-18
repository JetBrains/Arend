package com.jetbrains.jetpad.vclang.term.pattern.elimtree;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.Substitution;
import com.jetbrains.jetpad.vclang.term.expr.visitor.CompareVisitor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.SubstVisitor;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.visitor.ElimTreeNodeVisitor;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.DummyEquations;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.Equations;

import java.util.List;

public abstract class ElimTreeNode {
  private Clause myParent = null;

  Clause getParent() {
    return myParent;
  }

  public abstract <P, R> R accept(ElimTreeNodeVisitor<P, R> visitor, P params);

  @Override
  public boolean equals(Object obj) {
    return this == obj || obj instanceof ElimTreeNode && CompareVisitor.compare(DummyEquations.getInstance(), Equations.CMP.EQ, this, (ElimTreeNode) obj);
  }

  public ElimTreeNode subst(Substitution subst) {
    if (subst.getDomain().isEmpty()) {
      return this;
    }
    return accept(new SubstVisitor(subst), null);
  }

  public abstract ElimTreeNode matchUntilStuck(Substitution subst, boolean normalize);

  public ElimTreeNode matchUntilStuck(Substitution subst) {
    return matchUntilStuck(subst, true);
  }

  public abstract void updateLeavesMatched(List<Binding> context);

  public abstract LeafElimTreeNode match(List<Expression> expressions);

  public abstract Abstract.Definition.Arrow getArrow();

  void setParent(Clause parent) {
    myParent = parent;
  }
}
