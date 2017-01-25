package com.jetbrains.jetpad.vclang.naming.scope;

import com.jetbrains.jetpad.vclang.term.Abstract;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

public class EmptyScope implements Scope {
  @Override
  public Set<String> getNames() {
    return Collections.emptySet();
  }

  @Override
  public Abstract.Definition resolveName(String name) {
    return null;
  }

  @Override
  public Collection<? extends Abstract.ClassViewInstance> getInstances() {
    return Collections.emptySet();
  }

  @Override
  public Abstract.ClassViewInstance resolveInstance(Abstract.ClassView classView, Abstract.Definition classifyingDefinition) {
    return null;
  }

  @Override
  public Abstract.ClassViewInstance resolveInstance(Abstract.ClassDefinition classDefinition, Abstract.Definition classifyingDefinition) {
    return null;
  }
}
