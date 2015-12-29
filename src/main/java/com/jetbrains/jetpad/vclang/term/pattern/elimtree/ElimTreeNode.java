package com.jetbrains.jetpad.vclang.term.pattern.elimtree;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.Binding;
import com.jetbrains.jetpad.vclang.term.expr.visitor.NewCompareVisitor;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.visitor.ElimTreeNodeVisitor;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.DummyEquations;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.Equations;

import java.util.ArrayList;
import java.util.List;

public abstract class ElimTreeNode {
  private ConstructorClause myParent = null;

  public ConstructorClause getParent() {
    return myParent;
  }

  void setParent(ConstructorClause parent) {
    myParent = parent;
  }

  public abstract <P, R> R accept(ElimTreeNodeVisitor<P, R> visitor, P params);

  @Override
  public boolean equals(Object obj) {
    return this == obj || obj instanceof ElimTreeNode && NewCompareVisitor.compare(DummyEquations.getInstance(), Equations.CMP.EQ, new ArrayList<Binding>(), this, (ElimTreeNode) obj);
  }

  public static boolean compare(ElimTreeNode node1, ElimTreeNode node2, Equations equations, List<Binding> context) {
    return NewCompareVisitor.compare(equations, Equations.CMP.EQ, context, node1, node2);
  }

  public abstract Abstract.Definition.Arrow getArrow();

  @Override
  public String toString() {
    // TODO: better printing
    return ElimExpression.toElimExpression(this).toString();
  }
}
