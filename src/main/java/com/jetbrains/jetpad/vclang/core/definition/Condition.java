package com.jetbrains.jetpad.vclang.core.definition;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.core.pattern.elimtree.ElimTreeNode;

import java.util.List;

public class Condition implements Abstract.Condition {
  private final Constructor myConstructor;
  private final ElimTreeNode myElimTree;

  public Condition(Constructor constructor, ElimTreeNode elimTree) {
    myConstructor = constructor;
    myElimTree = elimTree;
  }

  public Constructor getConstructor() {
    return myConstructor;
  }

  @Override
  public String getConstructorName() {
    return myConstructor.getName();
  }

  @Override
  public List<Abstract.PatternArgument> getPatterns() {
    throw new UnsupportedOperationException();
  }

  public ElimTreeNode getElimTree() {
    return myElimTree;
  }

  @Override
  public Abstract.Expression getTerm() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setWellTyped(Condition condition) {

  }
}
