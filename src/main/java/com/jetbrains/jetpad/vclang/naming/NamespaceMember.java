package com.jetbrains.jetpad.vclang.naming;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.BaseDefinition;
import com.jetbrains.jetpad.vclang.term.definition.Definition;

public class NamespaceMember {
  public Namespace namespace;
  public Abstract.Definition abstractDefinition;
  public Definition definition;

  public NamespaceMember(Namespace namespace, Abstract.Definition abstractDefinition, Definition definition) {
    this.namespace = namespace;
    this.abstractDefinition = abstractDefinition;
    this.definition = definition;
  }

  public ResolvedName getResolvedName() {
    return namespace.getResolvedName();
  }

  public BaseDefinition getResolvedDefinition() {
    return definition == null ? abstractDefinition : definition;
  }

  public boolean isTypeChecked() {
    return definition != null;
  }
}
