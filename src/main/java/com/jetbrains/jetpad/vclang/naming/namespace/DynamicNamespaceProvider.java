package com.jetbrains.jetpad.vclang.naming.namespace;

import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;

public interface DynamicNamespaceProvider {
  Namespace forReferable(GlobalReferable referable);
}
