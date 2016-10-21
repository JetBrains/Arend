package com.jetbrains.jetpad.vclang.naming.scope;

import com.jetbrains.jetpad.vclang.error.GeneralError;
import com.jetbrains.jetpad.vclang.naming.error.DuplicateDefinitionError;
import com.jetbrains.jetpad.vclang.term.Abstract;

import java.util.HashSet;
import java.util.Set;

public class MergeScope implements Scope {
  private final Scope myScope1, myScope2;

  public MergeScope(Scope scope1, Scope scope2) {
    myScope1 = scope1;
    myScope2 = scope2;
  }

  @Override
  public Set<String> getNames() {
    Set<String> names = new HashSet<>(myScope1.getNames());
    names.addAll(myScope2.getNames());
    return names;
  }

  @Override
  public Abstract.Definition resolveName(String name) {
    final Abstract.Definition ref1 = myScope1.resolveName(name);
    final Abstract.Definition ref2 = myScope2.resolveName(name);

    if (ref1 == null) return ref2;
    if (ref2 == null) return ref1;

    throw new InvalidScopeException() {
      @Override
      public GeneralError toError() {
        return new DuplicateDefinitionError(ref1, ref2);
      }
    };
  }
}
