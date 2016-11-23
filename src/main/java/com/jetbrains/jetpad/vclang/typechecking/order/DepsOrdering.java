package com.jetbrains.jetpad.vclang.typechecking.order;

import com.jetbrains.jetpad.vclang.term.Abstract;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DepsOrdering extends BaseOrdering {
  private final Map<Abstract.Definition, Set<Abstract.Definition>> myDeps = new HashMap<>();

  public DepsOrdering(SCCListener listener) {
    super(listener);
  }

  @Override
  protected void dependsOn(Abstract.Definition def1, Abstract.Definition def2) {
    Set<Abstract.Definition> deps = myDeps.get(def2);
    if (deps == null) {
      deps = new HashSet<>();
      myDeps.put(def2, deps);
    }
    deps.add(def1);
  }

  public Map<Abstract.Definition, Set<Abstract.Definition>> getDependencies() {
    return myDeps;
  }
}
