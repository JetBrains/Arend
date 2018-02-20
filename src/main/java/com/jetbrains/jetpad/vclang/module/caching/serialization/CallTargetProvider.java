package com.jetbrains.jetpad.vclang.module.caching.serialization;

import com.jetbrains.jetpad.vclang.core.definition.Definition;

public interface CallTargetProvider {
  Definition getCallTarget(int index);

  default <DefinitionT extends Definition> DefinitionT getCallTarget(int index, Class<DefinitionT> cls) throws DeserializationException {
    Definition def = getCallTarget(index);
    if (!cls.isInstance(def)) throw new DeserializationException("Class mismatch");
    return cls.cast(def);
  }
}
