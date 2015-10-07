package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.module.Namespace;

public interface NamespaceMember {
  Namespace getNamespace();
  Name getName();
}
