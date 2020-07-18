package org.arend.naming.resolving;

import org.arend.naming.reference.Referable;
import org.arend.naming.scope.Scope;
import org.arend.term.NameRenaming;
import org.arend.term.NamespaceCommand;
import org.arend.term.concrete.Concrete;

import java.util.List;

public interface ResolverListener {
  default void referenceResolved(Concrete.Expression argument, Referable originalRef, Concrete.ReferenceExpression refExpr, List<Referable> resolvedRefs, Scope scope) {}
  default void patternResolved(Referable originalRef, Concrete.ConstructorPattern pattern, List<Referable> resolvedRefs) {}
  default void patternResolved(Concrete.NamePattern pattern) {}
  default void coPatternResolved(Concrete.CoClauseElement classFieldImpl, Referable originalRef, Referable referable, List<Referable> resolvedRefs) {}
  default void overriddenFieldResolved(Concrete.OverriddenField overriddenField, Referable originalRef, Referable referable, List<Referable> resolvedRefs) {}
  default void namespaceResolved(NamespaceCommand namespaceCommand, List<Referable> resolvedRefs) {}
  default void renamingResolved(NameRenaming renaming, Referable originalRef, Referable resolvedRef) {}
  default void metaResolved(Concrete.ReferenceExpression expression, Concrete.Expression result) {}

  default void beforeDefinitionResolved(Concrete.Definition definition) {}
  default void definitionResolved(Concrete.Definition definition) {}
}
