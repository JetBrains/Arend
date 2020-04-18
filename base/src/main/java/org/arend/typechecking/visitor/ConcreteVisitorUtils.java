package org.arend.typechecking.visitor;

import org.arend.term.concrete.Concrete;
import org.arend.term.concrete.ConcreteExpressionVisitor;

public interface ConcreteVisitorUtils {
  static <P, R> void visitPattern(Concrete.Pattern pattern, P params, ConcreteExpressionVisitor<P, R> visitor) {
    for (Concrete.TypedReferable typedReferable : pattern.getAsReferables()) {
      if (typedReferable.type != null) {
        typedReferable.type.accept(visitor, params);
      }
    }

    if (pattern instanceof Concrete.NamePattern) {
      Concrete.Expression type = ((Concrete.NamePattern) pattern).type;
      if (type != null) {
        type.accept(visitor, params);
      }
    } else if (pattern instanceof Concrete.ConstructorPattern) {
      for (Concrete.Pattern patternArg : ((Concrete.ConstructorPattern) pattern).getPatterns()) {
        visitPattern(patternArg, params, visitor);
      }
    } else if (pattern instanceof Concrete.TuplePattern) {
      for (Concrete.Pattern patternArg : ((Concrete.TuplePattern) pattern).getPatterns()) {
        visitPattern(patternArg, params, visitor);
      }
    }
  }
}
