package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.module.Root;
import com.jetbrains.jetpad.vclang.naming.NamespaceMember;
import com.jetbrains.jetpad.vclang.term.Abstract;

public interface BaseDefinition {
  String getName();
  Abstract.Definition.Precedence getPrecedence();

  class Helper {
    public static NamespaceMember toNamespaceMember(BaseDefinition ref) {
      if (ref instanceof Definition) {
        return ((Definition) ref).getResolvedName().toNamespaceMember();
      } else if (ref instanceof Abstract.Definition) {
        return abstractToNamespaceMember((Abstract.Definition) ref);
      } else {
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
}
