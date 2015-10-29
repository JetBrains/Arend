package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.module.Namespace;
import com.jetbrains.jetpad.vclang.module.RootModule;
import com.jetbrains.jetpad.vclang.term.Abstract;

import java.util.Arrays;

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

  public NamespaceMember toNamespaceMember() {
    return namespace == null ? new NamespaceMember(RootModule.ROOT, null, null) : namespace.getMember(name.name);
  }

  public Definition toDefinition() {
    return namespace.getDefinition(name.name);
  }

  public Abstract.Definition.Precedence toPrecedence() {
    return namespace.getMember(name.name).getPrecedence();
  }

  public Namespace toNamespace() {
    return namespace == null ? RootModule.ROOT : namespace.findChild(name.name);
  }

  @Override
  public String toString() {
    return (namespace == null || namespace == RootModule.ROOT ? "" : namespace + ".") + name.getPrefixName();
  }

  @Override
  public boolean equals(Object other) {
    return other == this || other instanceof ResolvedName
        && name.equals(((ResolvedName) other).name)
        && namespace == ((ResolvedName) other).namespace;
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(new Object[]{namespace, name == null ? null : name.name});
  }
}
