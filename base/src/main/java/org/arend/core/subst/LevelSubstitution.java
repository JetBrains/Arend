package org.arend.core.subst;

import org.arend.core.context.binding.Variable;
import org.arend.core.sort.Level;

public interface LevelSubstitution {
  boolean isEmpty();
  Level get(Variable variable);

  LevelSubstitution EMPTY = new LevelSubstitution() {
    @Override
    public boolean isEmpty() {
      return true;
    }

    @Override
    public Level get(Variable variable) {
      return null;
    }
  };
}
