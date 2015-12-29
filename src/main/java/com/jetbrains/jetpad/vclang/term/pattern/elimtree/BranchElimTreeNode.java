package com.jetbrains.jetpad.vclang.term.pattern.elimtree;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.Constructor;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.visitor.ElimTreeNodeVisitor;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class BranchElimTreeNode extends ElimTreeNode {
  private final int myIndex;
  private final Map<Constructor, ConstructorClause> myClauses = new HashMap<>();

  public BranchElimTreeNode(int index) {
    myIndex = index;
  }

  @Override
  public <P, R> R accept(ElimTreeNodeVisitor<P, R> visitor, P params) {
    return visitor.visitBranch(this, params);
  }

  @Override
  public Abstract.Definition.Arrow getArrow() {
    return Abstract.Definition.Arrow.LEFT;
  }

  public int getIndex() {
    return myIndex;
  }

  public void addClause(Constructor constructor, ElimTreeNode node) {
    myClauses.put(constructor, new ConstructorClause(constructor, node, this));
  }

  public ElimTreeNode getChild(Constructor constructor) {
    return myClauses.containsKey(constructor) ? myClauses.get(constructor).getChild() : null;
  }

  public Collection<ConstructorClause> getConstructorClauses() {
    return myClauses.values();
  }
}
