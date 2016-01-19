package com.jetbrains.jetpad.vclang.term.pattern.elimtree;

import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.definition.Constructor;

import java.util.List;

public class ConstructorClause {
  private final Constructor myConstructor;
  private final DependentLink myParameters;
  private ElimTreeNode myChild;
  private final BranchElimTreeNode myParent;
  private final List<Binding> myTailBindings;

  ConstructorClause(Constructor constructor, DependentLink parameters, List<Binding> tailBindings, ElimTreeNode child, BranchElimTreeNode parent) {
    myConstructor = constructor;
    myParameters = parameters;
    myTailBindings = tailBindings;
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

  public List<Binding> getTailBindings() {
    return myTailBindings;
  }

  public DependentLink getParameters() {
    return myParameters;
  }
}
