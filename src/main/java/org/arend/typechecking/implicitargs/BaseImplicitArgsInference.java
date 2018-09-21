package org.arend.typechecking.implicitargs;

import org.arend.core.expr.type.ExpectedType;
import org.arend.term.concrete.Concrete;
import org.arend.typechecking.visitor.CheckTypeVisitor;

public class BaseImplicitArgsInference implements ImplicitArgsInference {
  protected final CheckTypeVisitor myVisitor;

  protected BaseImplicitArgsInference(CheckTypeVisitor visitor) {
    myVisitor = visitor;
  }

  @Override
  public CheckTypeVisitor.TResult infer(Concrete.AppExpression expr, ExpectedType expectedType) {
    return null;
  }

  @Override
  public CheckTypeVisitor.TResult inferTail(CheckTypeVisitor.TResult fun, ExpectedType expectedType, Concrete.Expression expr) {
    return null;
  }
}
