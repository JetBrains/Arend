package org.arend.module.serialization;

import org.arend.core.context.binding.FieldLevelVariable;
import org.arend.core.context.binding.LevelVariable;
import org.arend.core.definition.Definition;
import org.arend.ext.serialization.DeserializationException;
import org.arend.naming.reference.MetaReferable;

public interface CallTargetProvider {
  Definition getCallTarget(int index) throws DeserializationException;
  MetaReferable getMetaCallTarget(int index) throws DeserializationException;
  FieldLevelVariable.LevelField getLevelCallTarget(int index) throws DeserializationException;

  default <DefinitionT extends Definition> DefinitionT getCallTarget(int index, Class<DefinitionT> cls) throws DeserializationException {
    Definition def = getCallTarget(index);
    if (!cls.isInstance(def)) {
      throw new DeserializationException("Class mismatch\nExpected class: " + cls.getName() + "\nActual class: " + def.getClass().getName());
    }
    return cls.cast(def);
  }
}
