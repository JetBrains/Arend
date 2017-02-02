package com.jetbrains.jetpad.vclang.naming.scope;

import com.jetbrains.jetpad.vclang.error.GeneralError;
import com.jetbrains.jetpad.vclang.naming.error.DuplicateDefinitionError;
import com.jetbrains.jetpad.vclang.term.Abstract;

import java.util.*;

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

  private <T extends Abstract.Definition> T choose(final T ref1, final T ref2) {
    if (ref1 == null) return ref2;
    if (ref2 == null) return ref1;

    throw new InvalidScopeException() {
      @Override
      public GeneralError toError() {
        return new DuplicateDefinitionError(ref1, ref2);
      }
    };
  }

  @Override
  public Abstract.Definition resolveName(String name) {
    return choose(myScope1.resolveName(name), myScope2.resolveName(name));
  }

  @Override
  public Collection<? extends Abstract.ClassViewInstance> getInstances() {
    List<Abstract.ClassViewInstance> instances = new ArrayList<>(myScope1.getInstances());
    instances.addAll(myScope2.getInstances());
    return instances;
  }
}
