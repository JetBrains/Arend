package com.jetbrains.jetpad.vclang.naming.scope;

import com.jetbrains.jetpad.vclang.term.definition.Referable;

import java.util.Set;

public interface Scope {
  Set<String> getNames();
  Referable resolveName(String name);
}
