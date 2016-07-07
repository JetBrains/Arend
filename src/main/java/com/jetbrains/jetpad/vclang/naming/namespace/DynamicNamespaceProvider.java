package com.jetbrains.jetpad.vclang.naming.namespace;

import com.jetbrains.jetpad.vclang.term.Abstract;

public interface DynamicNamespaceProvider {
  Namespace forClass(Abstract.ClassDefinition classDefinition);
}
