package com.jetbrains.jetpad.vclang.term.pattern.elimtree.visitor;

import com.jetbrains.jetpad.vclang.term.pattern.elimtree.BranchElimTreeNode;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.EmptyElimTreeNode;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.LeafElimTreeNode;

public interface ElimTreeNodeVisitor<P, R> {
  R visitBranch(BranchElimTreeNode branchNode, P params);
  R visitLeaf(LeafElimTreeNode leafNode, P params);
  R visitEmpty(EmptyElimTreeNode emptyNode, P params);
}
