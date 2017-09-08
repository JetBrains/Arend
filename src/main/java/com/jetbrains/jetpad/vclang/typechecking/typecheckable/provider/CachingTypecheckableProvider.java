package com.jetbrains.jetpad.vclang.typechecking.typecheckable.provider;

import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.term.Concrete;

import java.util.HashMap;
import java.util.Map;

public class CachingTypecheckableProvider implements TypecheckableProvider {
  private final TypecheckableProvider myProvider;
  private final Map<GlobalReferable, Concrete.ReferableDefinition> myCache = new HashMap<>();

  public CachingTypecheckableProvider(TypecheckableProvider provider) {
    myProvider = provider;
  }

  @Override
  public Concrete.ReferableDefinition getTypecheckable(GlobalReferable referable) {
    Concrete.ReferableDefinition definition = myCache.get(referable);
    if (definition != null) {
      return definition;
    }

    definition = myProvider.getTypecheckable(referable);
    myCache.put(referable, definition);
    return definition;
  }

  public void setTypecheckable(GlobalReferable referable, Concrete.ReferableDefinition typecheckable) {
    myCache.put(referable, typecheckable);
  }
}
