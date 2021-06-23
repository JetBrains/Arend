package org.arend.core.context.binding.inference;

import org.arend.core.context.binding.LevelVariable;
import org.arend.ext.core.ops.CMP;
import org.arend.term.concrete.Concrete;

public class InferenceLevelVariable implements LevelVariable {
  private final LvlType myType;
  private final boolean myUniverseLike;
  private final Concrete.SourceNode mySourceNode;

  public InferenceLevelVariable(LvlType type, boolean isUniverseLike, Concrete.SourceNode sourceNode) {
    myType = type;
    myUniverseLike = isUniverseLike;
    mySourceNode = sourceNode;
  }

  @Override
  public LvlType getType() {
    return myType;
  }

  @Override
  public LevelVariable max(LevelVariable other) {
    return this == other ? this : null;
  }

  @Override
  public LevelVariable min(LevelVariable other) {
    return this == other ? this : null;
  }

  @Override
  public boolean compare(LevelVariable other, CMP cmp) {
    return this == other;
  }

  public boolean isUniverseLike() {
    return myUniverseLike;
  }

  public Concrete.SourceNode getSourceNode() {
    return mySourceNode;
  }

  @Override
  public String toString() {
    return myType == LvlType.PLVL ? "?p" : "?h";
  }
}
