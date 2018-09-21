package org.arend.typechecking.implicitargs;

import org.arend.core.expr.type.ExpectedType;
import org.arend.term.concrete.Concrete;
import org.arend.typechecking.visitor.CheckTypeVisitor;
import org.arend.core.expr.type.ExpectedType;

public interface ImplicitArgsInference {
  CheckTypeVisitor.TResult infer(Concrete.AppExpression expr, ExpectedType expectedType);
  CheckTypeVisitor.TResult inferTail(CheckTypeVisitor.TResult fun, ExpectedType expectedType, Concrete.Expression expr);
}
