package org.arend.core.subst;

import org.arend.core.context.binding.LevelVariable;
import org.arend.core.context.binding.Variable;
import org.arend.core.sort.Level;
import org.arend.core.sort.Sort;

public class StdLevelSubstitution implements LevelSubstitution {
  private final Level myPLevel;
  private final Level myHLevel;

  public StdLevelSubstitution(Level pLevel, Level hLevel) {
    myPLevel = pLevel;
    myHLevel = hLevel;
  }

  public StdLevelSubstitution(Sort sort) {
    myPLevel = sort.getPLevel();
    myHLevel = sort.getHLevel();
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
