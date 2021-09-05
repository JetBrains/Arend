package org.arend.module.serialization;

import org.arend.core.context.binding.FieldLevelVariable;
import org.arend.core.definition.Definition;
import org.arend.naming.reference.TCReferable;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class SimpleCallTargetIndexProvider implements CallTargetIndexProvider {
  private final LinkedHashMap<Object, Integer> myCallTargets = new LinkedHashMap<>();

  @Override
  public int getDefIndex(Definition definition) {
    return getDefIndex(definition.getRef());
  }

  @Override
  public int getDefIndex(TCReferable definition) {
    return myCallTargets.computeIfAbsent(definition, k -> myCallTargets.size() + 1);
  }

  @Override
  public int getDefIndex(FieldLevelVariable.LevelField levelField) {
    return myCallTargets.computeIfAbsent(levelField, k -> myCallTargets.size() + 1);
  }

  public Collection<? extends Map.Entry<Object, Integer>> getCallTargets() {
    return myCallTargets.entrySet();
  }
}
