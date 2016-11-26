package com.jetbrains.jetpad.vclang.module.caching.serialization;

import com.jetbrains.jetpad.vclang.term.definition.Definition;

public interface CalltargetProvider {
  <DefinitionT extends Definition> DefinitionT getCalltarget(int index);

  abstract class BaseCalltargetProvider implements CalltargetProvider {
    protected abstract Definition getDef(int index);

    @Override
    public <DefinitionT extends Definition> DefinitionT getCalltarget(int index) {
      // TODO[serial]: this code is bogus
      try {
        //noinspection unchecked
        return (DefinitionT) getDef(index);
      } catch (ClassCastException ignored) {
        throw new IllegalStateException();
      }
    }
  }
}
