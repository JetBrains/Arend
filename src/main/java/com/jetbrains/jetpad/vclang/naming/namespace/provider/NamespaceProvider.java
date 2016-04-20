package com.jetbrains.jetpad.vclang.naming.namespace.provider;

import com.jetbrains.jetpad.vclang.naming.namespace.Namespace;
import com.jetbrains.jetpad.vclang.term.Abstract;

public interface NamespaceProvider {
  Namespace forDefinition(Abstract.Definition definition);
}
