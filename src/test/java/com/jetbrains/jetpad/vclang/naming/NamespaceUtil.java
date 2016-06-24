package com.jetbrains.jetpad.vclang.naming;

import com.jetbrains.jetpad.vclang.naming.namespace.SimpleDynamicNamespaceProvider;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.ClassDefinition;
import com.jetbrains.jetpad.vclang.term.definition.Referable;

import static com.jetbrains.jetpad.vclang.naming.NameResolverTestCase.DEFAULT_NAME_RESOLVER;

public class NamespaceUtil {
  public static Referable get(Referable ref, String path) {
    for (String n : path.split("\\.")) {
      Referable oldref = ref;

      ref = DEFAULT_NAME_RESOLVER.staticNamespaceFor(oldref).resolveName(n);
      if (ref != null) continue;

      if (oldref instanceof Abstract.ClassDefinition) {
        ref = SimpleDynamicNamespaceProvider.INSTANCE.forClass((Abstract.ClassDefinition) oldref).resolveName(n);
      } else if (oldref instanceof ClassDefinition) {
        ref = ((ClassDefinition) oldref).getInstanceNamespace().resolveName(n);
      }
      if (ref == null) return null;
    }
    return ref;
  }
}
