package com.jetbrains.jetpad.vclang.core.subst;

import com.jetbrains.jetpad.vclang.core.context.binding.Variable;
import com.jetbrains.jetpad.vclang.core.sort.Level;

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
