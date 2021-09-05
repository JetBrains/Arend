package org.arend.ext.core.level;

import org.arend.ext.variable.Variable;

public interface LevelSubstitution {
  boolean isEmpty();
  CoreLevel get(Variable variable);
  LevelSubstitution subst(LevelSubstitution substitution);

  LevelSubstitution EMPTY = new LevelSubstitution() {
    @Override
    public boolean isEmpty() {
      return true;
    }

    @Override
    public CoreLevel get(Variable variable) {
      return null;
    }

    @Override
    public LevelSubstitution subst(LevelSubstitution substitution) {
      return this;
    }
  };
}
