package com.jetbrains.jetpad.vclang.naming.namespace;

import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;

import java.util.Set;

public interface Namespace {
  Set<String> getNames();
  GlobalReferable resolveName(String name);
}
