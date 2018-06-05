package com.jetbrains.jetpad.vclang.module.serialization;

import com.jetbrains.jetpad.vclang.core.definition.Definition;

public interface CallTargetProvider {
  Definition getCallTarget(int index);

  default <DefinitionT extends Definition> DefinitionT getCallTarget(int index, Class<DefinitionT> cls) throws DeserializationException {
    Definition def = getCallTarget(index);
    if (def == null) {
      throw new DeserializationException("Wrong index");
    }
    if (!cls.isInstance(def)) {
      throw new DeserializationException("Class mismatch\nExpected class: " + cls.getName() + "\nActual class: " + def.getClass().getName());
    }
    return cls.cast(def);
  }
}
