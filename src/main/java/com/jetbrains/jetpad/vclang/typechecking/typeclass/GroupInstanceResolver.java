package com.jetbrains.jetpad.vclang.typechecking.typeclass;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.naming.NameResolver;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.naming.resolving.GroupResolver;
import com.jetbrains.jetpad.vclang.naming.scope.Scope;
import com.jetbrains.jetpad.vclang.typechecking.typecheckable.provider.ConcreteProvider;
import com.jetbrains.jetpad.vclang.typechecking.typeclass.provider.InstanceProviderSet;
import com.jetbrains.jetpad.vclang.typechecking.typeclass.provider.SimpleInstanceProvider;

public class GroupInstanceResolver extends GroupResolver {
  private final InstanceProviderSet myInstanceProviderSet;
  private final ConcreteProvider myConcreteProvider;

  public GroupInstanceResolver(NameResolver nameResolver, ErrorReporter errorReporter, InstanceProviderSet instanceProviderSet, ConcreteProvider concreteProvider) {
    super(nameResolver, errorReporter);
    myInstanceProviderSet = instanceProviderSet;
    myConcreteProvider = concreteProvider;
  }

  @Override
  protected void processReferable(GlobalReferable referable, Scope scope) {
    myInstanceProviderSet.setProvider(referable, new SimpleInstanceProvider(scope, myConcreteProvider));
  }
}
