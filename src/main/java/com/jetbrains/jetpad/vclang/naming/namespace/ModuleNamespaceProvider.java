package com.jetbrains.jetpad.vclang.naming.namespace;

import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;

// TODO[abstract]: Do we still need this?
public interface ModuleNamespaceProvider {
  ModuleNamespace forReferable(GlobalReferable referable);
  ModuleNamespace root();
}
