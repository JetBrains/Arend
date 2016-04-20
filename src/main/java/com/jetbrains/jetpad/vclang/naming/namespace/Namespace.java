package com.jetbrains.jetpad.vclang.naming.namespace;

import com.jetbrains.jetpad.vclang.term.definition.Referable;

import java.util.Set;

public interface Namespace {
  Set<String> getNames();
  Referable resolveName(String name);

  Referable resolveInstanceName(String name);
}
