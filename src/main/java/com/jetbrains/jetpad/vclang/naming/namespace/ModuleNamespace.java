package com.jetbrains.jetpad.vclang.naming.namespace;

import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;

import java.util.Set;

public interface ModuleNamespace {
  Set<String> getNames();
  ModuleNamespace getSubmoduleNamespace(String submodule);
  GlobalReferable getRegisteredClass(); // TODO[abstract]: return Namespace instead
}
