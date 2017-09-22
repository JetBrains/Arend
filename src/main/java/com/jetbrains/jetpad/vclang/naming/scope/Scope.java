package com.jetbrains.jetpad.vclang.naming.scope;

import com.jetbrains.jetpad.vclang.naming.reference.Referable;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete;

import java.util.Collection;

public interface Scope {
  Collection<? extends Referable> getElements();
  Referable resolveName(String name);

  Collection<? extends Concrete.Instance> getInstances(); // TODO[abstract]: Replace Concrete.Instance with something else, idk
}
