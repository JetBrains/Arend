package com.jetbrains.jetpad.vclang.naming.namespace;

import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;

import javax.annotation.Nonnull;

public interface StaticNamespaceProvider {
  @Nonnull Namespace forReferable(GlobalReferable referable);
}
