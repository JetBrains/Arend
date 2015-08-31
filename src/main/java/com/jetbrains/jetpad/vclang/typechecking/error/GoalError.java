package com.jetbrains.jetpad.vclang.typechecking.error;

import com.jetbrains.jetpad.vclang.module.Namespace;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.Binding;
import com.jetbrains.jetpad.vclang.term.expr.Expression;

import java.util.ArrayList;
import java.util.List;

public class GoalError extends TypeCheckingError {
  private final List<Binding> myContext;
  private final Expression myType;

  public GoalError(Namespace namespace, List<Binding> context, Expression type, Abstract.PrettyPrintableSourceNode expression) {
    super(namespace, "Goal", expression, getNames(context));
    myContext = new ArrayList<>(context);
    myType = type;
  }

  @Override
  public String toString() {
    if (myContext.isEmpty() && myType == null) {
      return printPosition() + getMessage();
    }

    StringBuilder builder = new StringBuilder();
    builder.append(printPosition()).append(getMessage());
    if (myType != null) {
      builder.append("\n\tExpected type: ");
      List<String> names = new ArrayList<>(myContext.size());
      for (Binding binding : myContext) {
        names.add(binding.getName() == null ? null : binding.getName().name);
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
        names.add(binding.getName() == null ? null : binding.getName().name);
      }
    }

    return builder.toString();
  }
}
