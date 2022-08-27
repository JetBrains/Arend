package org.arend.module.serialization;

import org.arend.core.definition.Definition;
import org.arend.ext.serialization.DeserializationException;
import org.arend.naming.reference.MetaReferable;
import org.arend.naming.reference.TCReferable;

public interface CallTargetProvider {
  TCReferable getRef(int index) throws DeserializationException;
  Definition getCallTarget(int index) throws DeserializationException;
  MetaReferable getMetaCallTarget(int index) throws DeserializationException;

  default <DefinitionT extends Definition> DefinitionT getCallTarget(int index, Class<DefinitionT> cls) throws DeserializationException {
    Definition def = getCallTarget(index);
    if (!cls.isInstance(def)) {
      throw new DeserializationException("Class mismatch\nExpected class: " + cls.getName() + "\nActual class: " + def.getClass().getName());
    }
    return cls.cast(def);
  }
}
