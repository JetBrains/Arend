package com.jetbrains.jetpad.vclang.core.context.binding.inference;

import com.jetbrains.jetpad.vclang.core.context.binding.LevelBinding;
import com.jetbrains.jetpad.vclang.core.context.binding.LevelVariable;
import com.jetbrains.jetpad.vclang.term.Abstract;

public class InferenceLevelVariable implements LevelVariable {
  private final LevelBinding myLevelBinding;
  private final Abstract.SourceNode mySourceNode;

  public InferenceLevelVariable(LevelBinding lvl, Abstract.SourceNode sourceNode) {
    myLevelBinding = lvl;
    mySourceNode = sourceNode;
  }

  @Override
  public String getName() {
    return "?" + myLevelBinding.getName().substring(1);
  }

  @Override
  public LvlType getType() {
    return myLevelBinding.getType();
  }

  public Abstract.SourceNode getSourceNode() {
    return mySourceNode;
  }

  @Override
  public String toString() {
    return getName();
  }
}
