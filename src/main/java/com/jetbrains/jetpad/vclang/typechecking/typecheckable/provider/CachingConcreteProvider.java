package com.jetbrains.jetpad.vclang.typechecking.typecheckable.provider;

import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete;
import com.jetbrains.jetpad.vclang.term.concrete.ConcreteDefinitionVisitor;

import java.util.HashMap;
import java.util.Map;

public class CachingConcreteProvider implements ConcreteProvider {
  private ConcreteProvider myProvider;
  private final Map<GlobalReferable, Concrete.ReferableDefinition> myCache = new HashMap<>();
  public final static Concrete.ReferableDefinition NULL_DEFINITION = new Concrete.Definition(null) {
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
    Concrete.ReferableDefinition definition = myCache.computeIfAbsent(referable, ref -> {
      Concrete.ReferableDefinition def = myProvider.getConcrete(ref);
      return def == null ? NULL_DEFINITION : def;
    });
    return definition == NULL_DEFINITION ? null : definition;
  }

  public void setTypecheckable(GlobalReferable referable, Concrete.ReferableDefinition typecheckable) {
    myCache.put(referable, typecheckable);
  }
}
