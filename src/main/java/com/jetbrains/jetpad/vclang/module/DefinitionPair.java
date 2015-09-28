package com.jetbrains.jetpad.vclang.module;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.ClassDefinition;
import com.jetbrains.jetpad.vclang.term.definition.Definition;

public class DefinitionPair {
  public Namespace namespace;
  public Abstract.Definition abstractDefinition;
  public Definition definition;

  public DefinitionPair(Namespace namespace, Abstract.Definition abstractDefinition, Definition definition) {
    this.namespace = namespace;
    this.abstractDefinition = abstractDefinition;
    this.definition = definition;
  }

  public Abstract.Definition.Precedence getPrecedence() {
    return definition != null ? definition.getPrecedence() : abstractDefinition != null ? abstractDefinition.getPrecedence() : null;
  }

  public boolean isTypeChecked() {
    return definition != null && definition.getUniverse() != null;
  }

  public void setLocalNamespace(Namespace localNamespace) {
    if (localNamespace == null) {
      return;
    }

    assert definition == null;
    ClassDefinition classDefinition = new ClassDefinition(localNamespace);
    classDefinition.setUniverse(null);
    classDefinition.setLocalNamespace(localNamespace);
    definition = classDefinition;
  }

  public Namespace getLocalNamespace() {
    if (definition instanceof ClassDefinition) {
      return ((ClassDefinition) definition).getLocalNamespace();
    }
    return null;
  }
}
