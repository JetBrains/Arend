package com.jetbrains.jetpad.vclang.core.context.binding.inference;

import com.jetbrains.jetpad.vclang.core.context.binding.LevelVariable;
import com.jetbrains.jetpad.vclang.term.Abstract;

public class InferenceLevelVariable implements LevelVariable {
  private final String myName;
  private final Abstract.SourceNode mySourceNode;
  private LvlType myType;

  public InferenceLevelVariable(String name, LvlType type, Abstract.SourceNode sourceNode) {
    myName = name;
    myType = type;
    mySourceNode = sourceNode;
  }

  @Override
  public String getName() {
    return myName;
  }

  @Override
  public LvlType getType() {
    return myType;
  }

  public Abstract.SourceNode getSourceNode() {
    return mySourceNode;
  }

  @Override
  public String toString() {
    return "?" + myName;
  }
}
