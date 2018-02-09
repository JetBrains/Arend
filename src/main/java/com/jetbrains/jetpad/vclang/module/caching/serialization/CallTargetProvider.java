package com.jetbrains.jetpad.vclang.module.caching.serialization;

import com.jetbrains.jetpad.vclang.core.definition.Definition;

public interface CallTargetProvider {
  Definition getCalltarget(int index);

  class Typed {
    private final CallTargetProvider myProvider;

    public Typed(CallTargetProvider provider) {
      myProvider = provider;
    }

    public <DefinitionT extends Definition> DefinitionT getCalltarget(int index, Class<DefinitionT> cls) throws DeserializationError {
      Definition def = myProvider.getCalltarget(index);
      if (!cls.isInstance(def)) throw new DeserializationError("Class mismatch");
      return cls.cast(def);
    }
  }
}
