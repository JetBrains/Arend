package com.jetbrains.jetpad.vclang.typechecking.nameresolver;

import com.jetbrains.jetpad.vclang.naming.Namespace;
import com.jetbrains.jetpad.vclang.naming.NamespaceMember;

public interface NameResolver {
  NamespaceMember locateName(String name);
  NamespaceMember getMember(Namespace parent, String name);

  class Helper {
    public static NamespaceMember locateName(NameResolver nameResolver, String name, boolean mustBeDefinition) {
      NamespaceMember result = nameResolver.locateName(name);
      if (result != null && (!mustBeDefinition || result.definition != null || result.abstractDefinition != null)) {
        return result;
      } else {
        return null;
      }
    }
  }
}
