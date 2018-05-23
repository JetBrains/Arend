package com.jetbrains.jetpad.vclang.naming.reference;

import com.jetbrains.jetpad.vclang.naming.scope.Scope;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface UnresolvedReference extends Referable {
  @Nullable Object getData();
  @Nonnull Referable resolve(Scope scope);
  @Nullable Referable tryResolve(Scope scope);
  @Nullable Concrete.Expression resolveArgument(Scope scope);
}
