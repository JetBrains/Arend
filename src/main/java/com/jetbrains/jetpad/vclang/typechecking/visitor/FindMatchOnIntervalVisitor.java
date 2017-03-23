package com.jetbrains.jetpad.vclang.typechecking.visitor;

import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.core.pattern.elimtree.BranchElimTreeNode;
import com.jetbrains.jetpad.vclang.core.pattern.elimtree.ConstructorClause;
import com.jetbrains.jetpad.vclang.core.pattern.elimtree.EmptyElimTreeNode;
import com.jetbrains.jetpad.vclang.core.pattern.elimtree.LeafElimTreeNode;
import com.jetbrains.jetpad.vclang.core.pattern.elimtree.visitor.ElimTreeNodeVisitor;

public class FindMatchOnIntervalVisitor implements ElimTreeNodeVisitor<Void, Boolean> {
  @Override
  public Boolean visitBranch(BranchElimTreeNode branchNode, Void params) {
    Expression type = branchNode.getReference().getType().getExpr();

    if (type.toDataCall() != null && type.toDataCall().getDefinition().matchesOnInterval()) {
      return true;
    }

    for (ConstructorClause clause : branchNode.getConstructorClauses()) {
      if (clause.getChild().accept(this, null)) {
        return true;
      }
    }

    return branchNode.getOtherwiseClause() != null && branchNode.getOtherwiseClause().getChild().accept(this, null);
  }

  @Override
  public Boolean visitLeaf(LeafElimTreeNode leafNode, Void params) {
    return false;
  }

  @Override
  public Boolean visitEmpty(EmptyElimTreeNode emptyNode, Void params) {
    return false;
  }
}
