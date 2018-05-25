package com.jetbrains.jetpad.vclang.core.expr;

import com.jetbrains.jetpad.vclang.core.definition.ClassField;
import com.jetbrains.jetpad.vclang.core.expr.visitor.ExpressionVisitor;
import com.jetbrains.jetpad.vclang.core.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.core.sort.Sort;

import java.util.Collections;
import java.util.List;

public class FieldCallExpression extends DefCallExpression {
  private final Expression myExpression;

  private FieldCallExpression(ClassField definition, Expression expression) {
    super(definition);
    myExpression = expression;
  }

  public static Expression make(ClassField definition, Expression thisExpr) {
    if (thisExpr.isInstance(NewExpression.class)) {
      Expression impl = thisExpr.cast(NewExpression.class).getExpression().getImplementation(definition, thisExpr);
      assert impl != null;
      return impl;
    } else {
      ErrorExpression errorExpr = thisExpr.checkedCast(ErrorExpression.class);
      if (errorExpr != null && errorExpr.getExpression() != null) {
        return new FieldCallExpression(definition, new ErrorExpression(null, errorExpr.getError()));
      } else {
        return new FieldCallExpression(definition, thisExpr);
      }
    }
  }

  public Expression getExpression() {
    return myExpression;
  }

  @Override
  public List<? extends Expression> getDefCallArguments() {
    return Collections.singletonList(myExpression);
  }

  @Override
  public Sort getSortArgument() {
    return Sort.PROP; // TODO[classes]
  }

  @Override
  public ClassField getDefinition() {
    return (ClassField) super.getDefinition();
  }

  @Override
  public <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitFieldCall(this, params);
  }

  @Override
  public boolean isWHNF() {
    if (myExpression.isInstance(NewExpression.class)) {
      return false;
    }
    Expression type = myExpression.getType();
    if (type == null) {
      return true;
    }
    type = type.normalize(NormalizeVisitor.Mode.WHNF);
    //noinspection SimplifiableIfStatement
    if (!type.isInstance(ClassCallExpression.class)) {
      return true;
    }
    return !type.cast(ClassCallExpression.class).isImplemented(getDefinition());
  }

  @Override
  public Expression getStuckExpression() {
    return myExpression.getStuckExpression();
  }
}
