package com.jetbrains.jetpad.vclang.term.error;

import com.jetbrains.jetpad.vclang.term.Abstract;

import java.util.ArrayList;
import java.util.List;

public class TypeCheckingError {
  private final String myMessage;
  private final Abstract.PrettyPrintableSourceNode myExpression;
  private final List<String> myNames;

  public TypeCheckingError(String message, Abstract.PrettyPrintableSourceNode expression, List<String> names) {
    myMessage = message;
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

  public Abstract.PrettyPrintableSourceNode getExpression() {
    return myExpression;
  }

  public String getMessage() {
    return myMessage;
  }

  protected String prettyPrint(Abstract.PrettyPrintableSourceNode expression) {
    StringBuilder builder = new StringBuilder();
    expression.prettyPrint(builder, myNames, Abstract.Expression.PREC);
    return builder.toString();
  }

  // TODO: Replace myExpression.toString() with pretty printing.
  @Override
  public String toString() {
    String msg = myMessage == null ? "Type checking error" : myMessage;
    if (myExpression == null) {
      return msg;
    } else {
      return msg + " in " + prettyPrint(myExpression);
    }
  }
}
