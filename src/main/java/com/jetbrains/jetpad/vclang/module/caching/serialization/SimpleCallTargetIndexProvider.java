package com.jetbrains.jetpad.vclang.module.caching.serialization;

import com.jetbrains.jetpad.vclang.core.definition.Definition;

import java.util.Collection;
import java.util.LinkedHashMap;

public class SimpleCallTargetIndexProvider implements CallTargetIndexProvider {
  private final LinkedHashMap<Definition, Integer> myCallTargets = new LinkedHashMap<>();

  @Override
  public int getDefIndex(Definition definition) {
    return myCallTargets.computeIfAbsent(definition, k -> myCallTargets.size());
  }

  public Collection<? extends Definition> getCallTargets() {
    return myCallTargets.keySet();
  }
}
