package com.jetbrains.jetpad.vclang.core.subst;

import com.jetbrains.jetpad.vclang.core.context.binding.Variable;
import com.jetbrains.jetpad.vclang.core.sort.Level;

import java.util.HashMap;
import java.util.Map;

public class SimpleLevelSubstitution implements LevelSubstitution {
  private final Map<Variable, Level> myLevels;

  public SimpleLevelSubstitution() {
    myLevels = new HashMap<>();
  }

  public void add(Variable variable, Level level) {
    myLevels.put(variable, level);
  }

  @Override
  public boolean isEmpty() {
    return myLevels.isEmpty();
  }

  @Override
  public Level get(Variable variable) {
    return myLevels.get(variable);
  }
}
