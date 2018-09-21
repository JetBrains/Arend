package org.arend.core.expr;

import org.arend.core.definition.ClassField;
import org.arend.core.expr.visitor.ExpressionVisitor;
import org.arend.core.expr.visitor.NormalizeVisitor;
import org.arend.core.sort.Sort;

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
    if (thisExpr.isInstance(NewExpression.class)) {
      Expression impl = thisExpr.cast(NewExpression.class).getExpression().getImplementation(definition, thisExpr);
      assert impl != null;
      return impl;
    } else {
      ErrorExpression errorExpr = thisExpr.checkedCast(ErrorExpression.class);
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
  public <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitFieldCall(this, params);
  }

  @Override
  public boolean isWHNF() {
    if (myArgument.isInstance(NewExpression.class)) {
      return false;
    }
    Expression type = myArgument.getType();
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
    return myArgument.getStuckExpression();
  }
}
