package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.module.Namespace;
import com.jetbrains.jetpad.vclang.module.RootModule;
import com.jetbrains.jetpad.vclang.term.Abstract;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ResolvedName {
  public Name name;
  public Namespace parent;

  public ResolvedName(Namespace parent, String name, Abstract.Definition.Fixity fixity) {
    this.name = new Name(name, fixity);
    this.parent = parent;
  }

  public ResolvedName(Namespace parent, String name) {
    this.name = new Name(name);
    this.parent = parent;
  }

  public ResolvedName(Namespace parent, Name name) {
    this(parent, name.name, name.fixity);
  }

  public NamespaceMember toNamespaceMember() {
    return parent == null ? new NamespaceMember(RootModule.ROOT, null, null) : parent.getMember(name.name);
  }

  public Definition toDefinition() {
    return parent.getDefinition(name.name);
  }

  public Abstract.Definition toAbstractDefinition() {
    return parent.getAbstractDefinition(name.name);
  }

  public Abstract.Definition.Precedence toPrecedence() {
    return parent.getMember(name.name).getPrecedence();
  }

  public Namespace toNamespace() {
    return parent == null ? RootModule.ROOT : parent.findChild(name.name);
  }

  @Override
  public String toString() {
    return (parent == null || parent == RootModule.ROOT ? "" : parent + ".") + name.getPrefixName();
  }

  @Override
  public boolean equals(Object other) {
    return other == this || other instanceof ResolvedName
        && name.equals(((ResolvedName) other).name)
        && parent == ((ResolvedName) other).parent;
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(new Object[]{parent, name == null ? null : name.name});
  }

  public static List<String> toPath(ResolvedName rn) {
    List<String> path = new ArrayList<>();
    for (; rn.parent != null; rn = rn.parent.getResolvedName()) {
      path.add(rn.name.name);
    }
    Collections.reverse(path);
    return path;
  }
}
