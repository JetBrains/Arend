package com.jetbrains.jetpad.vclang.term.pattern.elimtree;

import com.jetbrains.jetpad.vclang.term.definition.Constructor;

import java.util.List;

public class ConstructorClause {
  private ElimTreeNode myChild;
  private final BranchElimTreeNode myParent;
  private final Constructor myConstructor;
  private final List<String> myNames;

  public ConstructorClause(Constructor constructor, List<String> names, ElimTreeNode child, BranchElimTreeNode parent) {
    setChild(child);
    myParent = parent;
    myConstructor = constructor;
    myNames = names;
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

  public List<String> getNames() {
    return myNames;
  }
}
