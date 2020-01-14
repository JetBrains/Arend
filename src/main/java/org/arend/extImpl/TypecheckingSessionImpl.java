package org.arend.extImpl;

import org.arend.ext.concrete.ConcreteExpression;
import org.arend.ext.typechecking.CheckedExpression;
import org.arend.ext.typechecking.TypecheckingSession;
import org.arend.term.concrete.Concrete;
import org.arend.typechecking.visitor.CheckTypeVisitor;

import javax.annotation.Nonnull;

public class TypecheckingSessionImpl extends Disableable implements TypecheckingSession {
  private final CheckTypeVisitor myVisitor;

  public TypecheckingSessionImpl(CheckTypeVisitor visitor) {
    myVisitor = visitor;
  }

  @Nonnull
  @Override
  public CheckedExpression typecheck(@Nonnull ConcreteExpression expression) {
    checkEnabled();
    if (!(expression instanceof Concrete.Expression)) {
      throw new IllegalArgumentException();
    }
    return myVisitor.checkExpr((Concrete.Expression) expression, null);
  }
}
