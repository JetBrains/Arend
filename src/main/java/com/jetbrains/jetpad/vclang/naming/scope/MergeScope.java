package com.jetbrains.jetpad.vclang.naming.scope;

import com.jetbrains.jetpad.vclang.term.Abstract;

import java.util.HashSet;
import java.util.Set;

public class MergeScope implements Scope {
  private final Scope scope1, scope2;

  public MergeScope(Scope scope1, Scope scope2) {
    this.scope1 = scope1;
    this.scope2 = scope2;
  }

  @Override
  public Set<String> getNames() {
    Set<String> names = new HashSet<>(scope1.getNames());
    names.addAll(scope2.getNames());
    return names;
  }

  @Override
  public Abstract.Definition resolveName(String name) {
    Abstract.Definition ref1 = scope1.resolveName(name);
    Abstract.Definition ref2 = scope2.resolveName(name);

    if (ref1 == null) return ref2;
    else if (ref2 == null) return ref1;
    else return ref2;  // FIXME[error]
  }

}
