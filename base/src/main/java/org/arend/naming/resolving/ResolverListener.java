package org.arend.naming.resolving;

import org.arend.naming.reference.Referable;
import org.arend.term.NameRenaming;
import org.arend.term.NamespaceCommand;
import org.arend.term.concrete.Concrete;

import java.util.List;

public interface ResolverListener {
  void referenceResolved(Concrete.Expression argument, Referable originalRef, Concrete.ReferenceExpression refExpr, List<Referable> resolvedRefs);
  void patternResolved(Referable originalRef, Concrete.ConstructorPattern pattern, List<Referable> resolvedRefs);
  void coPatternResolved(Concrete.CoClauseElement classFieldImpl, Referable originalRef, Referable referable, List<Referable> resolvedRefs);
  void overriddenFieldResolved(Concrete.OverriddenField overriddenField, Referable originalRef, Referable referable, List<Referable> resolvedRefs);
  void namespaceResolved(NamespaceCommand namespaceCommand, List<Referable> resolvedRefs);
  void renamingResolved(NameRenaming renaming, Referable originalRef, Referable resolvedRef);

  void beforeDefinitionResolved(Concrete.Definition definition);
  void definitionResolved(Concrete.Definition definition);
}
