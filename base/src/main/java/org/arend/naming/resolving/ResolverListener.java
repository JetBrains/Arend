package org.arend.naming.resolving;

import org.arend.naming.reference.Referable;
import org.arend.naming.scope.Scope;
import org.arend.term.NameRenaming;
import org.arend.term.NamespaceCommand;
import org.arend.term.concrete.Concrete;

import java.util.Collection;
import java.util.List;

public interface ResolverListener {
  default void bindingResolved(Referable binding) {}
  default void referenceResolved(Concrete.Expression argument, Referable originalRef, Concrete.ReferenceExpression refExpr, List<Referable> resolvedRefs, Scope scope) {}
  default void patternParsed(Concrete.ConstructorPattern pattern) {}
  default void patternResolved(Referable originalRef, Referable newRef, Concrete.Pattern pattern, List<Referable> resolvedRefs) {}
  default void coPatternResolved(Concrete.CoClauseElement classFieldImpl, Referable originalRef, Referable referable, List<Referable> resolvedRefs) {}
  default void overriddenFieldResolved(Concrete.OverriddenField overriddenField, Referable originalRef, Referable referable, List<Referable> resolvedRefs) {}
  default void namespaceResolved(NamespaceCommand namespaceCommand, List<Referable> resolvedRefs) {}
  default void renamingResolved(NameRenaming renaming, Referable originalRef, Referable resolvedRef) {}
  default void metaResolved(Concrete.ReferenceExpression expression, List<Concrete.Argument> arguments, Concrete.Expression result, Concrete.Coclauses coclauses, Concrete.FunctionClauses clauses) {}
  default void levelResolved(Referable originalRef, Concrete.VarLevelExpression refExpr, Referable resolvedRef, Collection<Referable> availableRefs) {}

  default void beforeDefinitionResolved(Concrete.ResolvableDefinition definition) {}
  default void definitionResolved(Concrete.ResolvableDefinition definition) {}
}
