package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.definition.Binding;
import com.jetbrains.jetpad.vclang.term.definition.Name;
import com.jetbrains.jetpad.vclang.term.definition.TypedBinding;
import com.jetbrains.jetpad.vclang.term.expr.arg.Argument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TelescopeArgument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;
import com.jetbrains.jetpad.vclang.term.expr.visitor.ExpressionVisitor;

import java.util.ArrayList;
import java.util.List;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.Pi;
import static com.jetbrains.jetpad.vclang.term.expr.arg.Utils.trimToSize;

public class LamExpression extends Expression {
  private final List<TelescopeArgument> myArguments;
  private final Expression myBody;

  public LamExpression(List<TelescopeArgument> arguments, Expression body) {
    myArguments = arguments;
    myBody = body;
  }

  public List<TelescopeArgument> getArguments() {
    return myArguments;
  }

  public Expression getBody() {
    return myBody;
  }

  @Override
  public Expression getType(List<Binding> context) {
    int origSize = context.size();
    List<TypeArgument> resultArgs = new ArrayList<>(myArguments.size());
    for (Argument argument : myArguments) {
      if (!(argument instanceof TypeArgument)) return null;
      if (argument instanceof TelescopeArgument) {
        for (String name : ((TelescopeArgument) argument).getNames()) {
          context.add(new TypedBinding(name, ((TelescopeArgument) argument).getType()));
        }
      } else {
        context.add(new TypedBinding((Name) null, ((TypeArgument) argument).getType()));
      }
      resultArgs.add((TypeArgument) argument);
    }

    Expression resultCodomain = myBody.getType(context);
    trimToSize(context, origSize);
    return Pi(resultArgs, resultCodomain);
  }

  @Override
  public <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitLam(this, params);
  }
}
