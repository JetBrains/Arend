package com.jetbrains.jetpad.vclang.term.expr.subst;

import com.jetbrains.jetpad.vclang.term.context.binding.Variable;
import com.jetbrains.jetpad.vclang.term.expr.sort.Level;

import java.util.Collections;
import java.util.Map;

public class LevelSubstitution {
  private final Map<Variable, Level> myLevels;

  public LevelSubstitution() {
    myLevels = Collections.emptyMap();
  }

  public LevelSubstitution(Map<Variable, Level> levels) {
    myLevels = levels;
  }

  public boolean isEmpty() {
    return myLevels.isEmpty();
  }

  public Level get(Variable variable) {
    return myLevels.get(variable);
  }
}
