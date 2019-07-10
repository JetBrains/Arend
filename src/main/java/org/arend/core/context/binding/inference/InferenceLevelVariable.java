package org.arend.core.context.binding.inference;

import org.arend.core.context.binding.LevelVariable;
import org.arend.term.concrete.Concrete;

public class InferenceLevelVariable implements LevelVariable {
  private final LvlType myType;
  private final boolean myUniverseLike;
  private final Concrete.SourceNode mySourceNode;

  public static final InferenceLevelVariable UNKNOWN_PVAR = new InferenceLevelVariable(LvlType.PLVL, false, null);
  public static final InferenceLevelVariable UNKNOWN_HVAR = new InferenceLevelVariable(LvlType.HLVL, false, null);

  public InferenceLevelVariable(LvlType type, boolean isUniverseLike, Concrete.SourceNode sourceNode) {
    myType = type;
    myUniverseLike = isUniverseLike;
    mySourceNode = sourceNode;
  }

  @Override
  public LvlType getType() {
    return myType;
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
