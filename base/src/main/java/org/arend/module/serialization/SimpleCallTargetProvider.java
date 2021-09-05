package org.arend.module.serialization;

import org.arend.core.context.binding.FieldLevelVariable;
import org.arend.core.definition.Definition;
import org.arend.ext.serialization.DeserializationException;
import org.arend.naming.reference.MetaReferable;
import org.arend.naming.reference.TCDefReferable;
import org.arend.naming.reference.TCReferable;

import java.util.HashMap;
import java.util.Map;

public class SimpleCallTargetProvider implements CallTargetProvider {
  private final Map<Integer, Object> myMap = new HashMap<>();

  @Override
  public Definition getCallTarget(int index) throws DeserializationException {
    Object definition = myMap.get(index);
    if (definition == null) {
      throw new DeserializationException("Wrong index");
    }
    if (!(definition instanceof TCDefReferable)) {
      throw new DeserializationException("Not a definition");
    }
    return ((TCDefReferable) definition).getTypechecked();
  }

  @Override
  public MetaReferable getMetaCallTarget(int index) throws DeserializationException {
    Object definition = myMap.get(index);
    if (definition == null) {
      throw new DeserializationException("Wrong index");
    }
    if (!(definition instanceof MetaReferable)) {
      throw new DeserializationException("Not a meta");
    }
    return (MetaReferable) definition;
  }

  @Override
  public FieldLevelVariable.LevelField getLevelCallTarget(int index) throws DeserializationException {
    Object definition = myMap.computeIfAbsent(index, k -> new FieldLevelVariable.LevelField());
    if (!(definition instanceof FieldLevelVariable.LevelField)) {
      throw new DeserializationException("Not a level field");
    }
    return (FieldLevelVariable.LevelField) definition;
  }

  public void putCallTarget(int index, TCReferable callTarget) throws DeserializationException {
    if (!(callTarget instanceof MetaReferable || callTarget instanceof TCDefReferable && ((TCDefReferable) callTarget).getTypechecked() != null)) {
      throw new DeserializationException("Definition '" + callTarget.getRefName() + "' was not typechecked");
    }
    myMap.putIfAbsent(index, callTarget);
  }

  public void putCallTarget(int index, Definition callTarget) {
    myMap.putIfAbsent(index, callTarget.getRef());
  }
}
