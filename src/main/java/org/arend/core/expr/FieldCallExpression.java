package org.arend.core.expr;

import org.arend.core.definition.ClassField;
import org.arend.core.expr.visitor.ExpressionVisitor;
import org.arend.core.sort.Sort;
import org.arend.util.Decision;

import java.util.Collections;
import java.util.List;

public class FieldCallExpression extends DefCallExpression {
  private final Sort mySortArgument;
  private final Expression myArgument;

  private FieldCallExpression(ClassField definition, Sort sortArgument, Expression argument) {
    super(definition);
    mySortArgument = sortArgument;
    myArgument = argument;
  }

  public static Expression make(ClassField definition, Sort sortArgument, Expression thisExpr) {
    if (definition.isProperty()) {
      return new FieldCallExpression(definition, sortArgument, thisExpr);
    }

    NewExpression newExpr = thisExpr.cast(NewExpression.class);
    if (newExpr != null) {
      Expression impl = newExpr.getImplementation(definition);
      assert impl != null;
      return impl;
    } else {
      ErrorExpression errorExpr = thisExpr.cast(ErrorExpression.class);
      if (errorExpr != null && errorExpr.getExpression() != null) {
        return new FieldCallExpression(definition, sortArgument, new ErrorExpression(null, errorExpr.getError()));
      } else {
        return new FieldCallExpression(definition, sortArgument, thisExpr);
      }
    }
  }

  public Expression getArgument() {
    return myArgument;
  }

  @Override
  public List<? extends Expression> getDefCallArguments() {
    return Collections.singletonList(myArgument);
  }

  @Override
  public Sort getSortArgument() {
    return mySortArgument;
  }

  @Override
  public ClassField getDefinition() {
    return (ClassField) super.getDefinition();
  }

  @Override
  public boolean canBeConstructor() {
    return !getDefinition().isProperty();
  }

  @Override
  public <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitFieldCall(this, params);
  }

  @Override
  public Decision isWHNF() {
    if (getDefinition().isProperty()) {
      return Decision.YES;
    }
    if (myArgument.isInstance(NewExpression.class)) {
      return Decision.NO;
    }
    Expression type = myArgument.getType(false);
    if (type == null) {
      return Decision.MAYBE;
    }
    ClassCallExpression classCall = type.cast(ClassCallExpression.class);
    return classCall == null ? Decision.MAYBE : classCall.isImplemented(getDefinition()) ? Decision.NO : Decision.YES;
  }

  @Override
  public Expression getStuckExpression() {
    return myArgument.getStuckExpression();
  }
}
