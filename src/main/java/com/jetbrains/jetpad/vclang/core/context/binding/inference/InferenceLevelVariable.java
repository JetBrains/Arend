package com.jetbrains.jetpad.vclang.core.context.binding.inference;

import com.jetbrains.jetpad.vclang.core.context.binding.LevelVariable;
import com.jetbrains.jetpad.vclang.term.Concrete;

public class InferenceLevelVariable<T> implements LevelVariable {
  private final LvlType myType;
  private final Concrete.SourceNode<T> mySourceNode;

  public InferenceLevelVariable(LvlType type, Concrete.SourceNode<T> sourceNode) {
    myType = type;
    mySourceNode = sourceNode;
  }

  @Override
  public LvlType getType() {
    return myType;
  }

  public Concrete.SourceNode<T> getSourceNode() {
    return mySourceNode;
  }

  @Override
  public String toString() {
    return myType == LvlType.PLVL ? "?p" : "?h";
  }
}
