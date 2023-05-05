package org.arend.typechecking;

import org.arend.core.context.binding.LevelVariable;
import org.arend.core.context.binding.ParamLevelVariable;
import org.arend.naming.reference.LevelReferable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LevelContext {
  private final Map<LevelReferable, ParamLevelVariable> myPVars;
  private final Map<LevelReferable, ParamLevelVariable> myHVars;
  public final boolean isPBased;
  public final boolean isHBased;

  public LevelContext(Map<LevelReferable, ParamLevelVariable> pVars, Map<LevelReferable, ParamLevelVariable> hVars, boolean isPBased, boolean isHBased) {
    myPVars = pVars;
    myHVars = hVars;
    this.isPBased = isPBased;
    this.isHBased = isHBased;
  }

  public ParamLevelVariable getVariable(LevelReferable ref) {
    return switch (ref.getRefKind()) {
      case PLEVEL -> myPVars.get(ref);
      case HLEVEL -> myHVars.get(ref);
      case EXPR -> null;
    };
  }

  public LevelReferable get(int index, LevelVariable.LvlType type) {
    var vars = type == LevelVariable.LvlType.PLVL ? myPVars : myHVars;
    int i = 0;
    for (LevelReferable ref : vars.keySet()) {
      if (i++ == index) {
        return ref;
      }
    }
    return null;
  }

  public List<LevelReferable> getList(LevelVariable.LvlType type) {
    var vars = type == LevelVariable.LvlType.PLVL ? myPVars : myHVars;
    return new ArrayList<>(vars.keySet());
  }
}
