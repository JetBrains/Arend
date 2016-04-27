package com.jetbrains.jetpad.vclang.typechecking.error;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.expr.Expression;

import java.util.ArrayList;
import java.util.List;

public class GoalError extends TypeCheckingError {
  private final List<Binding> myContext;
  private final Expression myType;

  public GoalError(List<Binding> context, Expression type, Abstract.SourceNode expression) {
    super("Goal", expression);
    setLevel(Level.INFO);
    myContext = new ArrayList<>(context);
    myType = type;
  }

  public List<Binding> getContext() {
    return myContext;
  }

  public Expression getExpectedType() {
    return myType;
  }

  @Override
  public String toString() {
    if (myContext.isEmpty() && myType == null) {
      return printHeader() + getMessage();
    }

    StringBuilder builder = new StringBuilder();
    builder.append(printHeader()).append(getMessage());
    if (myType != null) {
      builder.append("\n\tExpected type: ");
      List<String> names = new ArrayList<>(myContext.size());
      for (Binding binding : myContext) {
        names.add(binding.getName() == null ? null : binding.getName());
      }
      myType.prettyPrint(builder, names, Abstract.Expression.PREC);
    }

    if (!myContext.isEmpty()) {
      builder.append("\n\tContext:");
      List<String> names = new ArrayList<>(myContext.size());
      for (Binding binding : myContext) {
        builder.append("\n\t\t").append(binding.getName() == null ? "_" : binding.getName()).append(" : ");
        Expression type = binding.getType();
        if (type != null) {
          type.prettyPrint(builder, names, Abstract.Expression.PREC);
        } else {
          builder.append("{!error}");
        }
        names.add(binding.getName() == null ? null : binding.getName());
      }
    }

    return builder.toString();
  }
}
