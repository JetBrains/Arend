package com.jetbrains.jetpad.vclang.naming.reference;

import com.jetbrains.jetpad.vclang.naming.NameResolver;
import com.jetbrains.jetpad.vclang.naming.scope.Scope;

import javax.annotation.Nonnull;

public interface UnresolvedReference extends Referable {
  @Nonnull Referable resolve(Scope scope, NameResolver nameResolver);
}
