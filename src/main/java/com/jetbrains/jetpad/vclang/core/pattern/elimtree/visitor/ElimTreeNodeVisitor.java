package com.jetbrains.jetpad.vclang.core.pattern.elimtree.visitor;

import com.jetbrains.jetpad.vclang.core.pattern.elimtree.BranchElimTreeNode;
import com.jetbrains.jetpad.vclang.core.pattern.elimtree.EmptyElimTreeNode;
import com.jetbrains.jetpad.vclang.core.pattern.elimtree.LeafElimTreeNode;

public interface ElimTreeNodeVisitor<P, R> {
  R visitBranch(BranchElimTreeNode branchNode, P params);
  R visitLeaf(LeafElimTreeNode leafNode, P params);
  R visitEmpty(EmptyElimTreeNode emptyNode, P params);
}
