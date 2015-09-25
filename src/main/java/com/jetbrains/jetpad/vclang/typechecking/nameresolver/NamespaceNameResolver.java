package com.jetbrains.jetpad.vclang.typechecking.nameresolver;

import com.jetbrains.jetpad.vclang.module.DefinitionPair;
import com.jetbrains.jetpad.vclang.module.Namespace;

public class NamespaceNameResolver implements NameResolver {
  private final Namespace myStaticNamespace;
  private final Namespace myDynamicNamespace;

  public NamespaceNameResolver(Namespace staticNamespace, Namespace dynamicNamespace) {
    myStaticNamespace = staticNamespace;
    myDynamicNamespace = dynamicNamespace;
  }

  protected Namespace getStaticNamespace() {
    return myStaticNamespace;
  }

  protected Namespace getDynamicNamespace() {
    return myDynamicNamespace;
  }

  @Override
  public DefinitionPair locateName(String name, boolean isStatic) {
    DefinitionPair result = !isStatic && myDynamicNamespace != null ? myDynamicNamespace.getMember(name) : null;
    return result != null ? result : myStaticNamespace != null ? myStaticNamespace.getMember(name) : null;
  }

  @Override
  public DefinitionPair getMember(Namespace parent, String name) {
    return null;
  }
}
