package org.arend.module.serialization;

import org.arend.core.definition.Definition;

import java.util.HashMap;
import java.util.Map;

public class SimpleCallTargetProvider implements CallTargetProvider {
  private final Map<Integer, Definition> myMap = new HashMap<>();

  @Override
  public Definition getCallTarget(int index) {
    return myMap.get(index);
  }

  public void putCallTarget(int index, Definition callTarget) {
    myMap.putIfAbsent(index, callTarget);
  }
}
