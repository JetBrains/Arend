package com.jetbrains.jetpad.vclang.core.subst;

import com.jetbrains.jetpad.vclang.core.definition.Referable;
import com.jetbrains.jetpad.vclang.core.sort.Level;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LevelSubstitution {
  private final Map<Referable, Level> myLevels;

  public LevelSubstitution() {
    myLevels = new HashMap<>();
  }

  public LevelSubstitution(Map<Referable, Level> levels) {
    myLevels = levels;
  }

  public LevelSubstitution(List<? extends Referable> vars, List<? extends Level> exprs) {
    assert vars.size() == exprs.size();
    myLevels = new HashMap<>();
    for (int i = 0; i < vars.size(); ++i) {
      myLevels.put(vars.get(i), exprs.get(i));
    }
  }

  public boolean isEmpty() {
    return myLevels.isEmpty();
  }

  public Level get(Referable variable) {
    return myLevels.get(variable);
  }

  public void add(Referable variable, Level level) {
    myLevels.put(variable, level);
  }

  public void add(LevelSubstitution subst) {
    myLevels.putAll(subst.myLevels);
  }
}
