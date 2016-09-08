package com.jetbrains.jetpad.vclang.naming;

import com.jetbrains.jetpad.vclang.term.definition.Definition;

public abstract class ResolvedName {
  public abstract String getName();
  public abstract ResolvedName getParent();
  public abstract NamespaceMember toNamespaceMember();

  public final Definition toDefinition() {
    return toNamespaceMember().definition;
  }

  public final Namespace toNamespace() {
    return toNamespaceMember().namespace;
  }

  public abstract String getFullName();

  @Override
  public String toString() {
    return getFullName();
  }
}
