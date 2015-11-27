package com.jetbrains.jetpad.vclang.term.pattern.elimtree.visitor;

import com.jetbrains.jetpad.vclang.term.pattern.elimtree.BranchElimTreeNode;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.ConstructorClause;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.ElimTreeNode;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.LeafElimTreeNode;

import java.util.Map;

public class ReplaceElimTreeNodeVisitor<U, V> implements ElimTreeNodeVisitor<U, ElimTreeNode<V>, Map<U, V>>{
  @Override
  public ElimTreeNode<V> visitBranch(BranchElimTreeNode<U> branchNode, Map<U, V> map) {
    BranchElimTreeNode<V> result = new BranchElimTreeNode<>(branchNode.getIndex());
    for (ConstructorClause<U> clause : branchNode.getConstructorClauses()) {
      result.addClause(clause.getConstructor(), clause.getChild().accept(this, map));
    }
    return result;
  }

  @Override
  public LeafElimTreeNode<V> visitLeaf(LeafElimTreeNode<U> leafNode, Map<U, V> map) {
    if (!map.containsKey(leafNode.getValue()))
      throw new IllegalStateException();
    return new LeafElimTreeNode<>(map.get(leafNode.getValue()));
  }
}
