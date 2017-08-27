package com.jetbrains.jetpad.vclang.typechecking.typecheckable.provider;

import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.term.Concrete;

import java.util.HashMap;
import java.util.Map;

public class CachingTypecheckableProvider<T> implements ModifiableTypecheckableProvider<T> {
  private final TypecheckableProvider<T> myProvider;
  private final Map<GlobalReferable, Concrete.ReferableDefinition<T>> myCache = new HashMap<>();

  public CachingTypecheckableProvider(TypecheckableProvider<T> provider) {
    myProvider = provider;
  }

  @Override
  public Concrete.ReferableDefinition<T> getTypecheckable(GlobalReferable referable) {
    Concrete.ReferableDefinition<T> definition = myCache.get(referable);
    if (definition != null) {
      return definition;
    }

    definition = myProvider.getTypecheckable(referable);
    myCache.put(referable, definition);
    return definition;
  }

  @Override
  public void setTypecheckable(GlobalReferable referable, Concrete.ReferableDefinition<T> typecheckable) {
    myCache.put(referable, typecheckable);
  }
}
