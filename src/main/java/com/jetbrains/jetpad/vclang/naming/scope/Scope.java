package com.jetbrains.jetpad.vclang.naming.scope;

import com.jetbrains.jetpad.vclang.term.Abstract;

import java.util.Set;

public interface Scope {
  Set<String> getNames();
  Abstract.Definition resolveName(String name);
}
