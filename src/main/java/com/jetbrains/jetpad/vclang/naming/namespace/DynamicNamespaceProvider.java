package com.jetbrains.jetpad.vclang.naming.namespace;

import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;

import javax.annotation.Nonnull;

public interface DynamicNamespaceProvider {
  @Nonnull Namespace forReferable(GlobalReferable referable);
}
