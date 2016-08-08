package com.jetbrains.jetpad.vclang.term.definition.visitor;

import com.jetbrains.jetpad.vclang.term.expr.DataCallExpression;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.*;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.visitor.ElimTreeNodeVisitor;

public class FindMatchOnIntervalVisitor implements ElimTreeNodeVisitor<Void, Boolean> {
  @Override
  public Boolean visitBranch(BranchElimTreeNode branchNode, Void params) {
    DataCallExpression dataCall = branchNode.getReference().getType().toDataCall();

    if (dataCall != null && dataCall.getDefinition().matchesOnInterval()) {
      return true;
    } /**/

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
