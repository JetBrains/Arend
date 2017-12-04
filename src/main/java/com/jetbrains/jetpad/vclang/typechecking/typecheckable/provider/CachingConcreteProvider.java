package com.jetbrains.jetpad.vclang.typechecking.typecheckable.provider;

import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete;
import com.jetbrains.jetpad.vclang.term.concrete.ConcreteDefinitionVisitor;

import java.util.HashMap;
import java.util.Map;

public class CachingConcreteProvider implements ConcreteProvider {
  private ConcreteProvider myProvider;
  private final Map<GlobalReferable, Concrete.ReferableDefinition> myCache = new HashMap<>();
  private final static Concrete.ReferableDefinition NULL_INSTANCE = new Concrete.Definition(null) {
    @Override
    public <P, R> R accept(ConcreteDefinitionVisitor<? super P, ? extends R> visitor, P params) {
      return null;
    }
  };

  public CachingConcreteProvider() {
    myProvider = EmptyConcreteProvider.INSTANCE;
  }

  public CachingConcreteProvider(ConcreteProvider provider) {
    myProvider = provider;
  }

  public void setProvider(ConcreteProvider provider) {
    myProvider = provider;
  }

  @Override
  public Concrete.ReferableDefinition getConcrete(GlobalReferable referable) {
    Concrete.ReferableDefinition definition = myCache.get(referable);
    if (definition != null) {
      return definition == NULL_INSTANCE ? null : definition;
    }

    definition = myProvider.getConcrete(referable);
    myCache.put(referable, definition == null ? NULL_INSTANCE : definition);
    return definition;
  }

  public void setTypecheckable(GlobalReferable referable, Concrete.ReferableDefinition typecheckable) {
    myCache.put(referable, typecheckable);
  }
}
