package org.arend.core.subst;

import org.arend.core.context.binding.LevelVariable;
import org.arend.ext.variable.Variable;
import org.arend.core.sort.Level;

public class StdLevelSubstitution implements LevelSubstitution {
  private final Level myPLevel;
  private final Level myHLevel;

  public StdLevelSubstitution(Level pLevel, Level hLevel) {
    myPLevel = pLevel;
    myHLevel = hLevel;
  }

  @Override
  public boolean isEmpty() {
    return false;
  }

  @Override
  public Level get(Variable variable) {
    return variable == LevelVariable.PVAR ? myPLevel : variable == LevelVariable.HVAR ? myHLevel : null;
  }
}
