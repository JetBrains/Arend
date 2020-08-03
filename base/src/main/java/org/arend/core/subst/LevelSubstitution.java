package org.arend.core.subst;

import org.arend.ext.variable.Variable;
import org.arend.core.sort.Level;

public interface LevelSubstitution {
  boolean isEmpty();
  Level get(Variable variable);
  LevelSubstitution subst(LevelSubstitution substitution);

  LevelSubstitution EMPTY = new LevelSubstitution() {
    @Override
    public boolean isEmpty() {
      return true;
    }

    @Override
    public Level get(Variable variable) {
      return null;
    }

    @Override
    public LevelSubstitution subst(LevelSubstitution substitution) {
      return this;
    }
  };
}
