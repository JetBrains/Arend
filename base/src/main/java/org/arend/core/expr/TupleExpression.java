package org.arend.core.expr;

import org.arend.core.expr.visitor.ExpressionVisitor;
import org.arend.core.expr.visitor.ExpressionVisitor2;
import org.arend.ext.core.expr.CoreExpressionVisitor;
import org.arend.ext.core.expr.CoreTupleExpression;
import org.arend.util.Decision;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class TupleExpression extends Expression implements CoreTupleExpression {
  private final List<Expression> myFields;
  private final SigmaExpression myType;
  private Boolean myValue;

  public TupleExpression(List<Expression> fields, SigmaExpression type) {
    myFields = fields;
    myType = type;
  }

  @Override
  public boolean isValue() {
    if (myValue == null) {
      myValue = areValues(myFields);
    }
    return myValue;
  }

  @NotNull
  @Override
  public List<Expression> getFields() {
    return myFields;
  }

  @NotNull
  @Override
  public SigmaExpression getSigmaType() {
    return myType;
  }

  @Override
  public <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitTuple(this, params);
  }

  @Override
  public <P1, P2, R> R accept(ExpressionVisitor2<? super P1, ? super P2, ? extends R> visitor, P1 param1, P2 param2) {
    return visitor.visitTuple(this, param1, param2);
  }

  @Override
  public <P, R> R accept(@NotNull CoreExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitTuple(this, params);
  }

  @Override
  public Decision isWHNF() {
    return Decision.YES;
  }

  @Override
  public Expression getStuckExpression() {
    return null;
  }
}
