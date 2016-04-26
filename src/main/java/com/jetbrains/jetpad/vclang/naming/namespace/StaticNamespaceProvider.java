package com.jetbrains.jetpad.vclang.naming.namespace;

import com.jetbrains.jetpad.vclang.term.Abstract;

public interface StaticNamespaceProvider {
  Namespace forDefinition(Abstract.Definition definition);
}
