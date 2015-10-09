package com.jetbrains.jetpad.vclang.typechecking.nameresolver;

import com.jetbrains.jetpad.vclang.module.Namespace;
import com.jetbrains.jetpad.vclang.term.definition.NamespaceMember;

public class DeepNamespaceNameResolver extends NamespaceNameResolver {
  public DeepNamespaceNameResolver(Namespace namespace) {
    super(namespace);
  }

  @Override
  public NamespaceMember locateName(String name) {
    NamespaceMember result = super.locateName(name);
    return result != null ? result : getNamespace().locateName(name);
  }

  @Override
  public NamespaceMember getMember(Namespace parent, String name) {
    return DummyNameResolver.getInstance().getMember(parent, name);
  }
}
