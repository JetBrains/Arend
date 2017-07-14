package com.jetbrains.jetpad.vclang.typechecking.order;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.typechecking.Typecheckable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CollectingDependencyListener implements DependencyListener {
  private final Map<Abstract.Definition, Set<Typecheckable>> myDependencies = new HashMap<>();

  @Override
  public void dependsOn(Typecheckable unit, Abstract.Definition def) {
    myDependencies.computeIfAbsent(def, k -> new HashSet<>()).add(unit);
  }

  public Map<Abstract.Definition, Set<Typecheckable>> getDependencies() {
    return myDependencies;
  }
}
