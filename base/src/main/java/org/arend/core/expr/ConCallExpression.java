package org.arend.core.expr;

import org.arend.core.definition.Constructor;
import org.arend.core.expr.visitor.ExpressionVisitor;
import org.arend.core.expr.visitor.ExpressionVisitor2;
import org.arend.core.subst.Levels;
import org.arend.ext.core.expr.CoreConCallExpression;
import org.arend.ext.core.expr.CoreExpressionVisitor;
import org.arend.prelude.Prelude;
import org.arend.util.Decision;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class ConCallExpression extends LeveledDefCallExpression implements CoreConCallExpression {
  private final List<Expression> myDataTypeArguments;
  private final List<Expression> myArguments;

  public ConCallExpression(Constructor definition, Levels levels, List<Expression> dataTypeArguments, List<Expression> arguments) {
    super(definition, levels);
    assert dataTypeArguments != null;
    myDataTypeArguments = dataTypeArguments;
    myArguments = arguments;
  }

  public static Expression make(Constructor constructor, Levels levels, List<Expression> dataTypeArguments, List<Expression> arguments) {
    if (constructor == Prelude.ZERO || constructor == Prelude.FIN_ZERO) {
      return new SmallIntegerExpression(0);
    }
    if (constructor == Prelude.FIN_SUC) {
      constructor = Prelude.SUC;
      dataTypeArguments = Collections.emptyList();
    }
    if (constructor == Prelude.SUC && !arguments.isEmpty()) {
      IntegerExpression intExpr = arguments.get(0).cast(IntegerExpression.class);
      if (intExpr != null) {
        return intExpr.suc();
      }
    }
    ConCallExpression result = new ConCallExpression(constructor, levels, dataTypeArguments, arguments);
    result.fixBoxes();
    return result;
  }

  public static ConCallExpression makeConCall(Constructor constructor, Levels levels, List<Expression> dataTypeArguments, List<Expression> arguments) {
    Expression conCall = make(constructor, levels, dataTypeArguments, arguments);
    if (!(conCall instanceof ConCallExpression)) {
      throw new IllegalArgumentException();
    }
    return (ConCallExpression) conCall;
  }

  @Override
  public Expression pred() {
    return getDefinition() == Prelude.SUC ? myArguments.get(0) : null;
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
    return getDefinition().getDataTypeExpression(getLevels(), myDataTypeArguments);
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
