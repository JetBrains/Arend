package com.jetbrains.jetpad.vclang.term.pattern.elimtree;

import com.jetbrains.jetpad.vclang.term.expr.visitor.CompareVisitor;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.visitor.ElimTreeNodeVisitor;

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
    if (this == obj) return true;
    if (!(obj instanceof ElimTreeNode)) return false;
    List<CompareVisitor.Equation> equations = new ArrayList<>(0);
    CompareVisitor.Result result = this.accept(new CompareVisitor(equations), (ElimTreeNode) obj);
    return result.isOK() != CompareVisitor.CMP.NOT_EQUIV && equations.size() == 0;
  }

  public static CompareVisitor.Result compare(ElimTreeNode node1, ElimTreeNode node2, List<CompareVisitor.Equation> equations) {
    return node1.accept(new CompareVisitor(equations), node2);
  }

  @Override
  public String toString() {
    // TODO: better printing
    return ElimExpression.toElimExpression(this).toString();
  }
}
