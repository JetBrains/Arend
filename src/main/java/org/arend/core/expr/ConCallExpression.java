package org.arend.core.expr;

import org.arend.core.definition.Constructor;
import org.arend.core.expr.visitor.ExpressionVisitor;
import org.arend.core.expr.visitor.ExpressionVisitor2;
import org.arend.core.sort.Sort;
import org.arend.ext.core.expr.CoreConCallExpression;
import org.arend.ext.core.expr.CoreExpressionVisitor;
import org.arend.prelude.Prelude;
import org.arend.util.Decision;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ConCallExpression extends DefCallExpression implements CoreConCallExpression {
  private final List<Expression> myDataTypeArguments;
  private final List<Expression> myArguments;

  public ConCallExpression(Constructor definition, Sort sortArgument, List<Expression> dataTypeArguments, List<Expression> arguments) {
    super(definition, sortArgument);
    assert dataTypeArguments != null;
    myDataTypeArguments = dataTypeArguments;
    myArguments = arguments;
  }

  public static Expression make(Constructor constructor, Sort sortArgument, List<Expression> dataTypeArguments, List<Expression> arguments) {
    if (constructor == Prelude.ZERO) {
      return new SmallIntegerExpression(0);
    }
    if (constructor == Prelude.SUC && !arguments.isEmpty()) {
      IntegerExpression intExpr = arguments.get(0).cast(IntegerExpression.class);
      if (intExpr != null) {
        return intExpr.suc();
      }
    }
    return new ConCallExpression(constructor, sortArgument, dataTypeArguments, arguments);
  }

  @NotNull
  @Override
  public List<Expression> getDataTypeArguments() {
    return myDataTypeArguments;
  }

  @NotNull
  @Override
  public List<Expression> getDefCallArguments() {
    return myArguments;
  }

  @NotNull
  @Override
  public Constructor getDefinition() {
    return (Constructor) super.getDefinition();
  }

  @Override
  public <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitConCall(this, params);
  }

  @Override
  public <P1, P2, R> R accept(ExpressionVisitor2<? super P1, ? super P2, ? extends R> visitor, P1 param1, P2 param2) {
    return visitor.visitConCall(this, param1, param2);
  }

  @Override
  public <P, R> R accept(@NotNull CoreExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitConCall(this, params);
  }

  public DataCallExpression getDataTypeExpression() {
    return getDefinition().getDataTypeExpression(getSortArgument(), myDataTypeArguments);
  }

  @Override
  public Decision isWHNF() {
    return getDefinition().getBody() != null ? getDefinition().getBody().isWHNF(myArguments) : Decision.YES;
  }

  @Override
  public Expression getStuckExpression() {
    return getDefinition().getBody() != null ? getDefinition().getBody().getStuckExpression(myArguments, this) : null;
  }
}
