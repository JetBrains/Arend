package org.arend.naming.resolving;

import org.arend.naming.reference.Referable;
import org.arend.term.concrete.Concrete;

public interface ResolverListener {
  void referenceResolved(Concrete.Expression argument, Referable originalRef, Concrete.ReferenceExpression refExpr);
  void patternResolved(Referable originalRef, Concrete.ConstructorPattern pattern);
  void definitionResolved(Concrete.Definition definition);
}
