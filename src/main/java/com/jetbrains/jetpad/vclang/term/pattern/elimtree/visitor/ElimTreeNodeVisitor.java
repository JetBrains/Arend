package com.jetbrains.jetpad.vclang.term.pattern.elimtree.visitor;

import com.jetbrains.jetpad.vclang.term.pattern.elimtree.BranchElimTreeNode;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.LeafElimTreeNode;

public interface ElimTreeNodeVisitor<T, R, P> {
  R visitBranch(BranchElimTreeNode<T> branchNode, P params);
  R visitLeaf(LeafElimTreeNode<T> leafNode, P params);
}
