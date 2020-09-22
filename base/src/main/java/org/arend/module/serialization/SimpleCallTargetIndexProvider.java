package org.arend.module.serialization;

import org.arend.core.definition.Definition;
import org.arend.naming.reference.TCReferable;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class SimpleCallTargetIndexProvider implements CallTargetIndexProvider {
  private final LinkedHashMap<TCReferable, Integer> myCallTargets = new LinkedHashMap<>();

  @Override
  public int getDefIndex(Definition definition) {
    return getDefIndex(definition.getRef());
  }

  @Override
  public int getDefIndex(TCReferable definition) {
    return myCallTargets.computeIfAbsent(definition, k -> myCallTargets.size() + 1);
  }

  public Collection<? extends Map.Entry<TCReferable, Integer>> getCallTargets() {
    return myCallTargets.entrySet();
  }
}
