package org.arend.core.subst;

import org.arend.ext.variable.Variable;
import org.arend.core.sort.Level;

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

  @Override
  public LevelSubstitution subst(LevelSubstitution substitution) {
    SimpleLevelSubstitution result = new SimpleLevelSubstitution();
    for (Map.Entry<Variable, Level> entry : myLevels.entrySet()) {
      result.myLevels.put(entry.getKey(), entry.getValue().subst(substitution));
    }
    return result;
  }
}
