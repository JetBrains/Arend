package org.arend.core.expr;

import org.arend.core.definition.Constructor;
import org.arend.core.expr.visitor.ExpressionVisitor;
import org.arend.core.expr.visitor.ExpressionVisitor2;
import org.arend.ext.core.expr.CoreExpressionVisitor;
import org.arend.ext.core.expr.CoreIntegerExpression;
import org.arend.prelude.Prelude;
import org.arend.util.Decision;

import javax.annotation.Nonnull;

public abstract class IntegerExpression extends Expression implements CoreIntegerExpression {
  public abstract int getSmallInteger();

  public abstract IntegerExpression suc();

  public abstract IntegerExpression pred();

  public abstract boolean isZero();

  public abstract boolean isOne();

  public abstract boolean isEqual(IntegerExpression expr);

  public abstract int compare(IntegerExpression expr);

  public abstract int compare(int x);

  public abstract IntegerExpression plus(IntegerExpression expr);

  public abstract IntegerExpression plus(int num);

  public abstract IntegerExpression mul(IntegerExpression expr);

  public abstract ConCallExpression minus(IntegerExpression expr);

  public abstract IntegerExpression minus(int x);

  public abstract IntegerExpression div(IntegerExpression expr);

  public abstract IntegerExpression mod(IntegerExpression expr);

  public abstract TupleExpression divMod(IntegerExpression expr);

  @Override
  public <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitInteger(this, params);
  }

  @Override
  public <P1, P2, R> R accept(ExpressionVisitor2<? super P1, ? super P2, ? extends R> visitor, P1 param1, P2 param2) {
    return visitor.visitInteger(this, param1, param2);
  }

  @Override
  public <P, R> R accept(@Nonnull CoreExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitInteger(this, params);
  }

  @Override
  public Decision isWHNF() {
    return Decision.YES;
  }

  @Override
  public Expression getStuckExpression() {
    return null;
  }

  public boolean match(Constructor constructor) {
    return (constructor == Prelude.ZERO || constructor == Prelude.SUC) && (constructor == Prelude.ZERO) == isZero();
  }
}
