package com.jetbrains.jetpad.vclang.naming.namespace;

import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface Namespace {
  Collection<? extends GlobalReferable> getElements();
  GlobalReferable resolveName(String name);
}
