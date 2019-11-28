package org.arend.core.expr;

import org.arend.core.definition.ClassField;
import org.arend.core.expr.visitor.ExpressionVisitor;
import org.arend.core.sort.Sort;
import org.arend.util.Decision;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class NewExpression extends Expression {
  private final Expression myRenewExpression;
  private final ClassCallExpression myClassCall;

  private NewExpression(NewExpression renewExpression, ClassCallExpression classCall) {
    myRenewExpression = renewExpression;
    myClassCall = classCall;
  }

  public NewExpression(Expression renewExpression, ClassCallExpression classCall) {
    NewExpression newExpr = renewExpression == null ? null : renewExpression.cast(NewExpression.class);
    if (newExpr != null) {
      myRenewExpression = newExpr.myRenewExpression;
      Map<ClassField, AbsExpression> implementations = new HashMap<>();
      NewExpression myNewExpr = new NewExpression(newExpr, classCall);
      for (ClassField field : classCall.getDefinition().getFields()) {
        if (classCall.getDefinition().isImplemented(field)) {
          continue;
        }
        Expression impl = classCall.getImplementationHere(field, myNewExpr);
        if (impl == null) {
          impl = newExpr.myClassCall.getImplementationHere(field, newExpr);
        }
        implementations.put(field, new AbsExpression(null, impl));
      }
      myClassCall = new ClassCallExpression(classCall.getDefinition(), classCall.getSortArgument(), implementations, Sort.PROP, false);
    } else {
      myRenewExpression = renewExpression;
      myClassCall = classCall;
    }
  }

  @Nullable
  public Expression getRenewExpression() {
    return myRenewExpression;
  }

  public ClassCallExpression getClassCall() {
    return myClassCall;
  }

  public Expression getImplementationHere(ClassField field) {
    Expression impl = myClassCall.getImplementationHere(field, this);
    return impl != null ? impl : FieldCallExpression.make(field, myClassCall.getSortArgument(), myRenewExpression);
  }

  public Expression getImplementation(ClassField field) {
    Expression impl = myClassCall.getImplementation(field, this);
    return impl != null ? impl : FieldCallExpression.make(field, myClassCall.getSortArgument(), myRenewExpression);
  }

  @Override
  public <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitNew(this, params);
  }

  @Override
  public Decision isWHNF() {
    return Decision.YES;
  }

  @Override
  public Expression getStuckExpression() {
    return null;
  }

  @Override
  public ClassCallExpression getType() {
    if (myRenewExpression == null) {
      return myClassCall;
    }

    Map<ClassField, AbsExpression> implementations = new HashMap<>();
    for (ClassField field : myClassCall.getDefinition().getFields()) {
      if (myClassCall.getDefinition().isImplemented(field)) {
        continue;
      }
      Expression impl = myClassCall.getImplementationHere(field, this);
      if (impl == null) {
        impl = FieldCallExpression.make(field, myClassCall.getSortArgument(), myRenewExpression);
      }
      implementations.put(field, new AbsExpression(null, impl));
    }
    return new ClassCallExpression(myClassCall.getDefinition(), myClassCall.getSortArgument(), implementations, Sort.PROP, false);
  }
}
