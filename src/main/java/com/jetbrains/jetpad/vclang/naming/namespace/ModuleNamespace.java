package com.jetbrains.jetpad.vclang.naming.namespace;

import com.jetbrains.jetpad.vclang.term.Abstract;

import java.util.Set;

public interface ModuleNamespace {
  Set<String> getNames();
  ModuleNamespace getSubmoduleNamespace(String submodule);
  Abstract.ClassDefinition getRegisteredClass();
}
