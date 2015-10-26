package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.module.Namespace;
import com.jetbrains.jetpad.vclang.term.Abstract;

public class NamespaceMember {
  public Namespace namespace;
  public Abstract.Definition abstractDefinition;
  public Definition definition;

  public NamespaceMember(Namespace namespace, Abstract.Definition abstractDefinition, Definition definition) {
    this.namespace = namespace;
    this.abstractDefinition = abstractDefinition;
    this.definition = definition;
  }

  public Abstract.Definition.Precedence getPrecedence() {
    return definition != null ? definition.getPrecedence() : abstractDefinition != null ? abstractDefinition.getPrecedence() : null;
  }

  public ResolvedName getResolvedName() {
    return new ResolvedName(namespace.getParent(), namespace.getName());
  }

  public boolean isTypeChecked() {
    return abstractDefinition == null ||
        !(definition == null || definition instanceof ClassDefinition && definition.getUniverse() == null);
  }
}
