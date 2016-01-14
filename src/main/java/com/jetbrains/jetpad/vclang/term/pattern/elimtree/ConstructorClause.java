package com.jetbrains.jetpad.vclang.term.pattern.elimtree;

import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.definition.Constructor;

public class ConstructorClause {
  private final Constructor myConstructor;
  private final DependentLink myParameters;
  private ElimTreeNode myChild;
  private final BranchElimTreeNode myParent;

  public ConstructorClause(Constructor constructor, DependentLink parameters, ElimTreeNode child, BranchElimTreeNode parent) {
    myConstructor = constructor;
    myParameters = parameters;
    setChild(child);
    myParent = parent;
  }

  public BranchElimTreeNode getParent() {
    return myParent;
  }

  void setChild(ElimTreeNode child) {
    myChild = child;
    child.setParent(this);
  }

  public ElimTreeNode getChild() {
    return myChild;
  }

  public Constructor getConstructor() {
    return myConstructor;
  }

  public DependentLink getParameters() {
    return myParameters;
  }
}
