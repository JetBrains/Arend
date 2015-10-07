package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.module.DefinitionPair;
import com.jetbrains.jetpad.vclang.module.Namespace;
import com.jetbrains.jetpad.vclang.term.Abstract;

public class ResolvedName {
  public Name name;
  public Namespace namespace;

  public ResolvedName(Namespace namespace, String name, Abstract.Definition.Fixity fixity) {
    this.name = new Name(name, fixity);
    this.namespace = namespace;
  }

  public ResolvedName(Namespace namespace, String name) {
    this.name = new Name(name);
    this.namespace = namespace;
  }

  public ResolvedName(Namespace namespace, Name name) {
    this(namespace, name.name, name.fixity);
  }

  public Definition toDefinition() {
    return namespace.getDefinition(name.name);
  }

  public Abstract.Definition.Precedence toPrecedence() {
    DefinitionPair dp = namespace.getMember(name.name);
    return dp.definition != null ? dp.definition.getPrecedence() : dp.abstractDefinition != null ? dp.abstractDefinition.getPrecedence() : null;
  }

  public Namespace toNamespace() {
    return namespace.getChild(name);
  }

  @Override
  public boolean equals(Object other) {
    return other == this || other instanceof ResolvedName
        && name.equals(((ResolvedName) other).name)
        && namespace == ((ResolvedName) other).namespace;
  }
}
