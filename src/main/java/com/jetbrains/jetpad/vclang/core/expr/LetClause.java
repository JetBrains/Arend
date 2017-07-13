package com.jetbrains.jetpad.vclang.core.expr;

import com.jetbrains.jetpad.vclang.core.context.binding.NamedBinding;
import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.definition.Callable;
import com.jetbrains.jetpad.vclang.core.expr.type.Type;
import com.jetbrains.jetpad.vclang.core.expr.type.TypeExpression;
import com.jetbrains.jetpad.vclang.core.sort.Sort;

import java.util.List;

public class LetClause extends NamedBinding implements Callable {
  private Expression myExpression;

  public LetClause(String name, Expression expression) {
    super(name);
    myExpression = expression;
  }

  public Expression getExpression() {
    return myExpression;
  }

  public void setExpression(Expression expression) {
    myExpression = expression;
  }

  @Override
  public Type getType() {
    // TODO[newElim]
    return new TypeExpression(myExpression.getType(), Sort.SET0);
  }

  @Override
  public Expression getTypeWithParams(List<? super DependentLink> params, Sort sortArgument) {
    return myExpression.getType();
  }

  @Override
  public Expression getDefCall(Sort sortArgument, Expression thisExpr, List<Expression> arguments) {
    return new LetClauseCallExpression(this, arguments);
  }
}
