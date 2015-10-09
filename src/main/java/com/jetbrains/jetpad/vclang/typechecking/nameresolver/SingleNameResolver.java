package com.jetbrains.jetpad.vclang.typechecking.nameresolver;

import com.jetbrains.jetpad.vclang.module.Namespace;
import com.jetbrains.jetpad.vclang.term.definition.NamespaceMember;

public class SingleNameResolver implements NameResolver {
  private final String myName;
  private final NamespaceMember myNamespaceMember;

  public SingleNameResolver(String name, NamespaceMember namespaceMember) {
    myName = name;
    myNamespaceMember = namespaceMember;
  }

  @Override
  public NamespaceMember locateName(String name) {
    return myName.equals(name) ? myNamespaceMember : null;
  }

  @Override
  public NamespaceMember getMember(Namespace parent, String name) {
    return null;
  }
}
