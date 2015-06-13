package com.jetbrains.jetpad.vclang.term.error;

import com.jetbrains.jetpad.vclang.VcError;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Concrete;

import java.util.ArrayList;
import java.util.List;

public class TypeCheckingError extends VcError {
  private final Abstract.SourceNode myExpression;
  private final List<String> myNames;

  public TypeCheckingError(String message, Abstract.SourceNode expression, List<String> names) {
    super(message);
    myExpression = expression;
    myNames = names;
  }

  public static List<String> getNames(List<? extends Abstract.Binding> context) {
    List<String> names = new ArrayList<>(context.size());
    for (Abstract.Binding binding : context) {
      names.add(binding.getName());
    }
    return names;
  }

  public Abstract.SourceNode getExpression() {
    return myExpression;
  }

  protected String prettyPrint(Abstract.PrettyPrintableSourceNode expression) {
    StringBuilder builder = new StringBuilder();
    expression.prettyPrint(builder, myNames, Abstract.Expression.PREC);
    return builder.toString();
  }

  @Override
  public String toString() {
    String msg;
    if (myExpression instanceof Concrete.SourceNode) {
      Concrete.Position position = ((Concrete.SourceNode) myExpression).getPosition();
      msg = position.line + ":" + position.column + ": ";
    } else {
      msg = "";
    }

    msg += getMessage() == null ? "Type checking error" : getMessage();
    if (myExpression instanceof Abstract.PrettyPrintableSourceNode) {
      return msg + " in " + prettyPrint((Abstract.PrettyPrintableSourceNode) myExpression);
    } else {
      return msg;
    }
  }
}
