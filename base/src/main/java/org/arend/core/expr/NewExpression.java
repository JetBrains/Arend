package org.arend.core.expr;

import org.arend.core.definition.ClassField;
import org.arend.core.definition.UniverseKind;
import org.arend.core.expr.visitor.ExpressionVisitor;
import org.arend.core.expr.visitor.ExpressionVisitor2;
import org.arend.core.sort.Sort;
import org.arend.ext.core.expr.CoreExpressionVisitor;
import org.arend.ext.core.expr.CoreNewExpression;
import org.arend.prelude.Prelude;
import org.arend.util.Decision;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class NewExpression extends Expression implements CoreNewExpression {
  private final Expression myRenewExpression;
  private final ClassCallExpression myClassCall;

  private NewExpression(NewExpression renewExpression, ClassCallExpression classCall) {
    myRenewExpression = renewExpression;
    myClassCall = classCall;
  }

  // only for SubstVisitor
  public NewExpression(Expression renewExpression, ClassCallExpression classCall, boolean checkRenew) {
    if (checkRenew) {
      NewExpression newExpr = renewExpression == null ? null : renewExpression.cast(NewExpression.class);
      if (newExpr != null) {
        myRenewExpression = newExpr.myRenewExpression;
        Map<ClassField, Expression> implementations = new HashMap<>();
        NewExpression myNewExpr = new NewExpression(newExpr, classCall);
        for (ClassField field : classCall.getDefinition().getFields()) {
          if (classCall.getDefinition().isImplemented(field)) {
            continue;
          }
          Expression impl = classCall.getImplementationHere(field, myNewExpr);
          if (impl == null) {
            impl = newExpr.myClassCall.getImplementationHere(field, newExpr);
          }
          implementations.put(field, impl);
        }
        myClassCall = new ClassCallExpression(classCall.getDefinition(), classCall.getLevels(), implementations, Sort.PROP, UniverseKind.NO_UNIVERSES);
      } else {
        myRenewExpression = renewExpression;
        myClassCall = classCall;
      }
    } else {
      myRenewExpression = renewExpression;
      myClassCall = classCall;
    }
  }

  public NewExpression(Expression renewExpression, ClassCallExpression classCall) {
    this(renewExpression, classCall, true);
  }

  @Nullable
  public Expression getRenewExpression() {
    return myRenewExpression;
  }

  @NotNull
  @Override
  public ClassCallExpression getClassCall() {
    return myClassCall;
  }

  public Expression getImplementationHere(ClassField field) {
    Expression impl = myClassCall.getImplementationHere(field, this);
    return impl != null ? impl : FieldCallExpression.make(field, myClassCall.getLevels(), myRenewExpression);
  }

  public Expression getImplementation(ClassField field) {
    Expression impl = myClassCall.getImplementation(field, this);
    return impl != null ? impl : FieldCallExpression.make(field, myClassCall.getLevels(), myRenewExpression);
  }

  @Override
  public <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitNew(this, params);
  }

  @Override
  public <P1, P2, R> R accept(ExpressionVisitor2<? super P1, ? super P2, ? extends R> visitor, P1 param1, P2 param2) {
    return visitor.visitNew(this, param1, param2);
  }

  @Override
  public <P, R> R accept(@NotNull CoreExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitNew(this, params);
  }

  @Override
  public Decision isWHNF() {
    if (myClassCall.getDefinition() != Prelude.ARRAY) {
      return Decision.YES;
    }

    Expression length = getImplementation(Prelude.ARRAY_LENGTH).getUnderlyingExpression();
    return length instanceof IntegerExpression || length instanceof ConCallExpression ? Decision.NO : length.isWHNF();
  }

  @Override
  public Expression getStuckExpression() {
    return myClassCall.getDefinition() == Prelude.ARRAY ? getImplementation(Prelude.ARRAY_LENGTH).getStuckExpression() : null;
  }

  @NotNull
  @Override
  public ClassCallExpression getType() {
    if (myRenewExpression == null) {
      return myClassCall;
    }

    Map<ClassField, Expression> implementations = new HashMap<>();
    for (ClassField field : myClassCall.getDefinition().getFields()) {
      if (myClassCall.getDefinition().isImplemented(field)) {
        continue;
      }
      Expression impl = field.isProperty() ? null : myClassCall.getImplementationHere(field, this);
      if (impl == null) {
        impl = FieldCallExpression.make(field, myClassCall.getLevels(), myRenewExpression, false);
      }
      implementations.put(field, impl);
    }
    return new ClassCallExpression(myClassCall.getDefinition(), myClassCall.getLevels(), implementations, Sort.PROP, UniverseKind.NO_UNIVERSES);
  }

  @Override
  public @NotNull ClassCallExpression computeType() {
    return getType();
  }
}
