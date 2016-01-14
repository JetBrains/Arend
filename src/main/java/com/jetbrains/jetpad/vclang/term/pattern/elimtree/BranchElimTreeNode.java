package com.jetbrains.jetpad.vclang.term.pattern.elimtree;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.definition.Constructor;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.visitor.ElimTreeNodeVisitor;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class BranchElimTreeNode extends ElimTreeNode {
  private final Binding myReference;
  private final Map<Constructor, ConstructorClause> myClauses = new HashMap<>();

  public BranchElimTreeNode(Binding reference) {
    myReference = reference;
  }

  @Override
  public <P, R> R accept(ElimTreeNodeVisitor<P, R> visitor, P params) {
    return visitor.visitBranch(this, params);
  }

  @Override
  public Abstract.Definition.Arrow getArrow() {
    return Abstract.Definition.Arrow.LEFT;
  }

  public Binding getReference() {
    return myReference;
  }

  public void addClause(Constructor constructor, DependentLink parameters, ElimTreeNode node) {
    myClauses.put(constructor, new ConstructorClause(constructor, parameters, node, this));
  }

  public ConstructorClause getClause(Constructor constructor) {
    return myClauses.get(constructor);
  }

  public Collection<ConstructorClause> getConstructorClauses() {
    return myClauses.values();
  }
}
