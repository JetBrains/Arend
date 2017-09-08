package com.jetbrains.jetpad.vclang.naming.reference;

import com.jetbrains.jetpad.vclang.naming.NameResolver;
import com.jetbrains.jetpad.vclang.naming.scope.Scope;

import javax.annotation.Nullable;

public interface UnresolvedReference extends Referable {
  @Nullable Object getData();
  @Nullable Referable resolve(Scope scope, NameResolver nameResolver);
  @Nullable Referable resolveStatic(GlobalReferable enclosingClass, NameResolver nameResolver);
  @Nullable Referable resolveDynamic(GlobalReferable enclosingClass, NameResolver nameResolver);
}
