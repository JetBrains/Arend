package com.jetbrains.jetpad.vclang.naming.reference;

import com.jetbrains.jetpad.vclang.naming.NameResolver;
import com.jetbrains.jetpad.vclang.naming.scope.Scope;

import javax.annotation.Nullable;

public interface UnresolvedReference extends Referable {
  @Nullable Object getData();
  @Nullable Referable resolve(Scope scope, NameResolver nameResolver); // TODO[abstract]: Make @Nonnull
}
