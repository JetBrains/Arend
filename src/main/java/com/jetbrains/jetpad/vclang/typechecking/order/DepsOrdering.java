package com.jetbrains.jetpad.vclang.typechecking.order;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.typechecking.Typecheckable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DepsOrdering extends BaseOrdering {
  private final Map<Abstract.Definition, Set<Typecheckable>> myDeps = new HashMap<>();

  public DepsOrdering(SCCListener listener) {
    super(listener);
  }

  @Override
  protected void dependsOn(Typecheckable unit, Abstract.Definition def) {
    Set<Typecheckable> deps = myDeps.get(def);
    if (deps == null) {
      deps = new HashSet<>();
      myDeps.put(def, deps);
    }
    deps.add(unit);
  }

  public Map<Abstract.Definition, Set<Typecheckable>> getDependencies() {
    return myDeps;
  }
}
