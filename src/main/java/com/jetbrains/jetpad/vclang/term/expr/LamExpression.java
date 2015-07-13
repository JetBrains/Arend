package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.expr.arg.Argument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TelescopeArgument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;
import com.jetbrains.jetpad.vclang.term.expr.visitor.AbstractExpressionVisitor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.ExpressionVisitor;

import java.util.ArrayList;
import java.util.List;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.Pi;
import static com.jetbrains.jetpad.vclang.term.expr.arg.Utils.trimToSize;

public class LamExpression extends Expression implements Abstract.LamExpression {
  private final List<Argument> myArguments;
  private final Expression myBody;

  public LamExpression(List<Argument> arguments, Expression body) {
    myArguments = arguments;
    myBody = body;
  }

  @Override
  public List<Argument> getArguments() {
    return myArguments;
  }

  @Override
  public Expression getBody() {
    return myBody;
  }

  @Override
  public <T> T accept(ExpressionVisitor<? extends T> visitor) {
    return visitor.visitLam(this);
  }

  @Override
  public Expression getType(List<Expression> context) {
    int origSize = context.size();
    List<TypeArgument> resultArgs = new ArrayList<>(myArguments.size());
    for (Argument argument : myArguments) {
      if (!(argument instanceof TypeArgument)) return null;
      if (argument instanceof TelescopeArgument) {
        for (String ignored : ((TelescopeArgument) argument).getNames()) {
          context.add(((TelescopeArgument) argument).getType());
        }
      } else {
        context.add(((TypeArgument) argument).getType());
      }
      resultArgs.add((TypeArgument) argument);
    }

    Expression resultCodomain = myBody.getType(context);
    trimToSize(context, origSize);
    return Pi(resultArgs, resultCodomain);
  }

  @Override
  public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitLam(this, params);
  }
}
