package com.jetbrains.jetpad.vclang.naming;

import com.jetbrains.jetpad.vclang.module.Root;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.Referable;
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

  @Deprecated
  public static NamespaceMember toNamespaceMember(Referable ref) {
    if (ref instanceof Definition) {
      return ((Definition) ref).getResolvedName().toNamespaceMember();
    } else if (ref instanceof Abstract.Definition){
      return abstractToNamespaceMember((Abstract.Definition) ref);
    } else {
      // FIXME[referable]
      throw new IllegalStateException();
    }
  }

  private static NamespaceMember abstractToNamespaceMember(Abstract.Definition definition) {
    NamespaceMember parentMember = null;
    if (definition.getParentStatement() == null) {
      if (definition instanceof Abstract.Constructor) {
        parentMember = abstractToNamespaceMember(((Abstract.Constructor) definition).getDataType());
      } else
      if (definition instanceof Abstract.ClassDefinition) {
        Abstract.ClassDefinition module = (Abstract.ClassDefinition) definition;
        return Root.getModule(module.getModuleID());
      }
    } else {
      parentMember = abstractToNamespaceMember(definition.getParentStatement().getParentDefinition());
    }
    return parentMember == null ? null : parentMember.namespace.getMember(definition.getName());
  }
}
