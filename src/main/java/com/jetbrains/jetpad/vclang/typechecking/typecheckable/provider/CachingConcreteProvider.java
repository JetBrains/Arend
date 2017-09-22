package com.jetbrains.jetpad.vclang.typechecking.typecheckable.provider;

import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete;

import java.util.HashMap;
import java.util.Map;

public class CachingConcreteProvider implements ConcreteProvider {
  private final ConcreteProvider myProvider;
  private final Map<GlobalReferable, Concrete.ReferableDefinition> myCache = new HashMap<>();

  public CachingConcreteProvider(ConcreteProvider provider) {
    myProvider = provider;
  }

  @Override
  public Concrete.ReferableDefinition getConcrete(GlobalReferable referable) {
    Concrete.ReferableDefinition definition = myCache.get(referable);
    if (definition != null) {
      return definition;
    }

    definition = myProvider.getConcrete(referable);
    if (definition != null) {
      myCache.put(referable, definition);
    }
    return definition;
  }

  public void setTypecheckable(GlobalReferable referable, Concrete.ReferableDefinition typecheckable) {
    myCache.put(referable, typecheckable);
  }
}
