package com.jetbrains.jetpad.vclang.typechecking.error;

import com.jetbrains.jetpad.vclang.naming.ResolvedName;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.expr.Expression;

import java.util.ArrayList;
import java.util.List;

public class GoalError extends TypeCheckingError {
  private final List<Binding> myContext;
  private final Expression myType;

  public GoalError(ResolvedName resolvedName, List<Binding> context, Expression type, Abstract.SourceNode expression) {
    super(resolvedName, null, expression);
    myContext = new ArrayList<>(context);
    myType = type;
    setLevel(Level.GOAL);
  }

  public GoalError(List<Binding> context, Expression type, Abstract.SourceNode expression) {
    super(null, expression);
    myContext = new ArrayList<>(context);
    myType = type;
    setLevel(Level.GOAL);
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
      return printHeader();
    }

    StringBuilder builder = new StringBuilder();
    builder.append(printHeader());
    if (myType != null) {
      builder.append("\n\tExpected type: ");
      List<String> names = new ArrayList<>(myContext.size());
      for (Binding binding : myContext) {
        names.add(binding.getName() == null ? null : binding.getName());
      }
      myType.prettyPrint(builder, names, Abstract.Expression.PREC, 0);
    }

    if (!myContext.isEmpty()) {
      builder.append("\n\tContext:");
      List<String> names = new ArrayList<>(myContext.size());
      for (Binding binding : myContext) {
        builder.append("\n\t\t").append(binding.getName() == null ? "_" : binding.getName()).append(" : ");
        Expression type = binding.getType();
        if (type != null) {
          type.prettyPrint(builder, names, Abstract.Expression.PREC, 0);
        } else {
          builder.append("{!error}");
        }
        names.add(binding.getName() == null ? null : binding.getName());
      }
    }

    return builder.toString();
  }
}
