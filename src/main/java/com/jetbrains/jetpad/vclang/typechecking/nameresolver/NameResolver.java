package com.jetbrains.jetpad.vclang.typechecking.nameresolver;

import com.jetbrains.jetpad.vclang.module.Namespace;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.NamespaceMember;
import com.jetbrains.jetpad.vclang.typechecking.error.NotInScopeError;
import com.jetbrains.jetpad.vclang.typechecking.error.reporter.ErrorReporter;

public interface NameResolver {
  NamespaceMember locateName(String name);
  NamespaceMember getMember(Namespace parent, String name);

  class Helper {
    public static NamespaceMember locateName(NameResolver nameResolver, String name, Abstract.SourceNode node, ErrorReporter errorReporter, boolean mustBeDefinition) {
      NamespaceMember result = nameResolver.locateName(name);
      if (result != null && (!mustBeDefinition || result.definition != null || result.abstractDefinition != null)) {
        return result;
      } else {
        errorReporter.report(new NotInScopeError(node, name));
        return null;
      }
    }
  }
}
