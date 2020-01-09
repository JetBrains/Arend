package org.arend.core.expr;

import org.arend.core.definition.Constructor;
import org.arend.core.expr.visitor.ExpressionVisitor;
import org.arend.core.sort.Sort;
import org.arend.ext.core.expr.CoreConCallExpression;
import org.arend.prelude.Prelude;
import org.arend.util.Decision;

import javax.annotation.Nonnull;
import java.util.List;

public class ConCallExpression extends DefCallExpression implements CoreConCallExpression {
  private final Sort mySortArgument;
  private final List<Expression> myDataTypeArguments;
  private final List<Expression> myArguments;

  public ConCallExpression(Constructor definition, Sort sortArgument, List<Expression> dataTypeArguments, List<Expression> arguments) {
    super(definition);
    assert dataTypeArguments != null;
    mySortArgument = sortArgument;
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

  @Nonnull
  @Override
  public List<Expression> getDataTypeArguments() {
    return myDataTypeArguments;
  }

  @Nonnull
  @Override
  public Sort getSortArgument() {
    return mySortArgument;
  }

  @Nonnull
  @Override
  public List<? extends Expression> getDefCallArguments() {
    return myArguments;
  }

  @Nonnull
  @Override
  public Constructor getDefinition() {
    return (Constructor) super.getDefinition();
  }

  @Override
  public <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitConCall(this, params);
  }

  public DataCallExpression getDataTypeExpression() {
    return getDefinition().getDataTypeExpression(mySortArgument, myDataTypeArguments);
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
