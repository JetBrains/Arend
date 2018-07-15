package com.jetbrains.jetpad.vclang.core.expr;

import com.jetbrains.jetpad.vclang.core.definition.Constructor;
import com.jetbrains.jetpad.vclang.core.expr.visitor.ExpressionVisitor;
import com.jetbrains.jetpad.vclang.core.sort.Sort;
import com.jetbrains.jetpad.vclang.prelude.Prelude;

import java.math.BigInteger;
import java.util.List;

public class ConCallExpression extends DefCallExpression {
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
      return new IntegerExpression(BigInteger.ZERO);
    }
    if (constructor == Prelude.SUC && !arguments.isEmpty() && arguments.get(0).isInstance(IntegerExpression.class)) {
      return new IntegerExpression(arguments.get(0).cast(IntegerExpression.class).getInteger().add(BigInteger.ONE));
    }
    return new ConCallExpression(constructor, sortArgument, dataTypeArguments, arguments);
  }

  public List<Expression> getDataTypeArguments() {
    return myDataTypeArguments;
  }

  @Override
  public Sort getSortArgument() {
    return mySortArgument;
  }

  @Override
  public List<? extends Expression> getDefCallArguments() {
    return myArguments;
  }

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
  public boolean isWHNF() {
    //noinspection SimplifiableConditionalExpression
    return getDefinition().getBody() != null ? getDefinition().getBody().isWHNF(myArguments) : true;
  }

  @Override
  public Expression getStuckExpression() {
    return getDefinition().getBody() != null ? getDefinition().getBody().getStuckExpression(myArguments, this) : null;
  }
}
