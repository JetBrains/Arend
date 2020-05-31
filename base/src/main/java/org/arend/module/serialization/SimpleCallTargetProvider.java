package org.arend.module.serialization;

import org.arend.core.definition.Definition;
import org.arend.ext.serialization.DeserializationException;

import java.util.HashMap;
import java.util.Map;

public class SimpleCallTargetProvider implements CallTargetProvider {
  private final Map<Integer, Definition> myMap = new HashMap<>();

  @Override
  public Definition getCallTarget(int index) throws DeserializationException {
    Definition definition = myMap.get(index);
    if (definition == null) {
      throw new DeserializationException("Wrong index");
    }
    return definition;
  }

  public void putCallTarget(int index, Definition callTarget) {
    myMap.putIfAbsent(index, callTarget);
  }
}
