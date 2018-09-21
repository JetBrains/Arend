package org.arend.module.serialization;

import org.arend.core.definition.Definition;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class SimpleCallTargetIndexProvider implements CallTargetIndexProvider {
  private final LinkedHashMap<Definition, Integer> myCallTargets = new LinkedHashMap<>();

  @Override
  public int getDefIndex(Definition definition) {
    assert definition != null;
    return myCallTargets.computeIfAbsent(definition, k -> myCallTargets.size() + 1);
  }

  public Collection<? extends Map.Entry<Definition, Integer>> getCallTargets() {
    return myCallTargets.entrySet();
  }
}
