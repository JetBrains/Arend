package com.jetbrains.jetpad.vclang.naming.namespace;

import com.jetbrains.jetpad.vclang.term.Abstract;

public interface ModuleNamespace extends Namespace {
  ModuleNamespace getSubmoduleNamespace(String submodule);
  Abstract.ClassDefinition getRegisteredClass();
}
