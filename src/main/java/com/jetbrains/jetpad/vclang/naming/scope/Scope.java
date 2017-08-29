package com.jetbrains.jetpad.vclang.naming.scope;

import com.jetbrains.jetpad.vclang.naming.reference.Referable;
import com.jetbrains.jetpad.vclang.term.Concrete;

import java.util.Collection;
import java.util.Set;

public interface Scope {
  Set<String> getNames();
  Referable resolveName(String name);

  Collection<? extends Concrete.Instance> getInstances(); // TODO[abstract]: Replace Concrete.Instance with something else, idk
}
