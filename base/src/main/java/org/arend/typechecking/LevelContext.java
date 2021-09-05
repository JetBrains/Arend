package org.arend.typechecking;

import org.arend.core.context.binding.ParamLevelVariable;
import org.arend.naming.reference.LevelReferable;

import java.util.Map;

public class LevelContext {
  private final Map<LevelReferable, ParamLevelVariable> myVariables;
  public final boolean isPBased;
  public final boolean isHBased;

  public LevelContext(Map<LevelReferable, ParamLevelVariable> variables, boolean isPBased, boolean isHBased) {
    myVariables = variables;
    this.isPBased = isPBased;
    this.isHBased = isHBased;
  }

  public ParamLevelVariable getVariable(LevelReferable ref) {
    return myVariables.get(ref);
  }
}
