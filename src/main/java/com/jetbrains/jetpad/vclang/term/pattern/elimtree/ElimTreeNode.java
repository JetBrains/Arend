package com.jetbrains.jetpad.vclang.term.pattern.elimtree;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.expr.Substitution;
import com.jetbrains.jetpad.vclang.term.expr.factory.ConcreteExpressionFactory;
import com.jetbrains.jetpad.vclang.term.expr.visitor.CompareVisitor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.SubstVisitor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.ToAbstractVisitor;
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

  public static boolean compare(ElimTreeNode node1, ElimTreeNode node2, Equations equations, List<Binding> context) {
    return CompareVisitor.compare(equations, Equations.CMP.EQ, node1, node2);
  }

  public ElimTreeNode subst(Substitution subst) {
    return accept(new SubstVisitor(subst), null);
  }

  public abstract ElimTreeNode matchUntilStuck(Substitution subst);

  public abstract Abstract.Definition.Arrow getArrow();

  @Override
  public String toString() {
    return accept(new ToAbstractVisitor(new ConcreteExpressionFactory()), null).toString();
  }

  void setParent(Clause parent) {
    myParent = parent;
  }
}
