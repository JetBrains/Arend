package com.jetbrains.jetpad.vclang.typechecking.nameresolver;

import com.jetbrains.jetpad.vclang.module.DefinitionPair;
import com.jetbrains.jetpad.vclang.module.Namespace;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.typechecking.error.NotInScopeError;
import com.jetbrains.jetpad.vclang.typechecking.error.NotInStaticScopeError;
import com.jetbrains.jetpad.vclang.typechecking.error.reporter.ErrorReporter;

public interface NameResolver {
  DefinitionPair locateName(String name, boolean isStatic);
  DefinitionPair getMember(Namespace parent, String name);

  class Helper {
    public static DefinitionPair locateName(NameResolver nameResolver, String name, Abstract.SourceNode node, boolean isStatic, ErrorReporter errorReporter) {
      DefinitionPair member = nameResolver.locateName(name, isStatic);
      if (member != null) {
        return member;
      } else {
        if (isStatic) {
          member = nameResolver.locateName(name, false);
          if (member != null && (member.definition != null || member.abstractDefinition != null)) {
            errorReporter.report(new NotInStaticScopeError(node, name));
            return null;
          }
        }
        errorReporter.report(new NotInScopeError(node, name));
        return null;
      }
    }
  }
}
