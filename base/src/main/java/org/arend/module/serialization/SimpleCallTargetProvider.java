package org.arend.module.serialization;

import org.arend.core.definition.Definition;
import org.arend.ext.serialization.DeserializationException;
import org.arend.naming.reference.*;

import java.util.HashMap;
import java.util.Map;

public class SimpleCallTargetProvider implements CallTargetProvider {
  private final Map<Integer, TCReferable> myMap = new HashMap<>();

  @Override
  public TCReferable getRef(int index) throws DeserializationException {
    TCReferable ref = myMap.get(index);
    if (ref == null) {
      throw new DeserializationException("Wrong index");
    }
    return ref;
  }

  @Override
  public Definition getCallTarget(int index) throws DeserializationException {
    TCReferable ref = getRef(index);
    if (!(ref instanceof TCDefReferable)) {
      throw new DeserializationException("Not a definition");
    }
    return ((TCDefReferable) ref).getTypechecked();
  }

  @Override
  public MetaReferable getMetaCallTarget(int index) throws DeserializationException {
    Referable ref = getRef(index);
    if (!(ref instanceof MetaReferable)) {
      throw new DeserializationException("Not a meta");
    }
    return (MetaReferable) ref;
  }

  public void putCallTarget(int index, TCReferable callTarget) throws DeserializationException {
    if (!(callTarget instanceof MetaReferable || callTarget instanceof LevelReferable || callTarget instanceof TCDefReferable && ((TCDefReferable) callTarget).getTypechecked() != null)) {
      throw new DeserializationException("Definition '" + callTarget.getRefName() + "' was not typechecked");
    }
    myMap.putIfAbsent(index, callTarget);
  }

  public void putCallTarget(int index, Definition callTarget) {
    myMap.putIfAbsent(index, callTarget.getRef());
  }
}
