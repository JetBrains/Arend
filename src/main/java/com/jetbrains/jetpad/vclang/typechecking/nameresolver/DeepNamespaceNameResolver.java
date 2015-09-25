package com.jetbrains.jetpad.vclang.typechecking.nameresolver;

import com.jetbrains.jetpad.vclang.module.DefinitionPair;
import com.jetbrains.jetpad.vclang.module.Namespace;

public class DeepNamespaceNameResolver extends NamespaceNameResolver {
  public DeepNamespaceNameResolver(Namespace staticNamespace, Namespace dynamicNamespace) {
    super(staticNamespace, dynamicNamespace);
  }

  @Override
  public DefinitionPair locateName(String name, boolean isStatic) {
    DefinitionPair result = !isStatic && getDynamicNamespace() != null ? getDynamicNamespace().locateName(name) : null;
    return result != null ? result : getStaticNamespace().locateName(name);
  }

  @Override
  public DefinitionPair getMember(Namespace parent, String name) {
    return parent.getMember(name);
  }
}
