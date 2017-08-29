package com.jetbrains.jetpad.vclang.typechecking.typeclass;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.naming.NameResolver;
import com.jetbrains.jetpad.vclang.naming.resolving.GroupResolver;
import com.jetbrains.jetpad.vclang.naming.scope.Scope;
import com.jetbrains.jetpad.vclang.term.Group;
import com.jetbrains.jetpad.vclang.typechecking.typeclass.provider.InstanceProviderSet;
import com.jetbrains.jetpad.vclang.typechecking.typeclass.provider.SimpleInstanceProvider;

public class GroupInstanceResolver<T> extends GroupResolver<T> {
  private final InstanceProviderSet myInstanceProviderSet;

  public GroupInstanceResolver(NameResolver nameResolver, ErrorReporter<T> errorReporter, InstanceProviderSet instanceProviderSet) {
    super(nameResolver, errorReporter);
    myInstanceProviderSet = instanceProviderSet;
  }

  @Override
  protected void processGroup(Group group, Scope scope) {
    myInstanceProviderSet.setProvider(group.getReferable(), new SimpleInstanceProvider(scope));
  }
}
