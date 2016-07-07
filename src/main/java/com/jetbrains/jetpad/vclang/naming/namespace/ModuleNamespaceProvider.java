package com.jetbrains.jetpad.vclang.naming.namespace;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.ClassDefinition;

public interface ModuleNamespaceProvider {
  ModuleNamespace forModule(Abstract.ClassDefinition definition);
  ModuleNamespace forModule(ClassDefinition definition);
  ModuleNamespace root();
}
