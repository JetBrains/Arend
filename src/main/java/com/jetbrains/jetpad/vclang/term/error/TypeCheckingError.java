package com.jetbrains.jetpad.vclang.term.error;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.PrettyPrintable;
import com.jetbrains.jetpad.vclang.term.definition.Definition;

import java.util.ArrayList;
import java.util.List;

public class TypeCheckingError {
  private final Definition myParent;
  private final String myMessage;
  private final Abstract.SourceNode myExpression;
  private final List<String> myNames;

  public TypeCheckingError(Definition parent, String message, Abstract.SourceNode expression, List<String> names) {
    myParent = parent;
    myMessage = message;
    myExpression = expression;
    myNames = names;
  }

  public String getMessage() {
    return myMessage;
  }

  public Definition getParent() {
    return myParent;
  }

  public static List<String> getNames(List<? extends Abstract.Binding> context) {
    List<String> names = new ArrayList<>(context.size());
    for (Abstract.Binding binding : context) {
      names.add(binding.getName() == null ? null : binding.getName().name);
    }
    return names;
  }

  public Abstract.SourceNode getExpression() {
    return myExpression;
  }

  protected String prettyPrint(PrettyPrintable expression) {
    StringBuilder builder = new StringBuilder();
    expression.prettyPrint(builder, myNames, Abstract.Expression.PREC);
    return builder.toString();
  }

  protected String printPosition() {
    String msg = myParent == null ? "" : myParent.getFullName() + ":";
    if (myExpression instanceof Concrete.SourceNode) {
      Concrete.Position position = ((Concrete.SourceNode) myExpression).getPosition();
      msg += position.line + ":" + position.column + ":";
    }
    return msg + " ";
  }

  @Override
  public String toString() {
    String msg = printPosition();
    msg += getMessage() == null ? "Type checking error" : getMessage();
    if (myExpression instanceof Abstract.PrettyPrintableSourceNode) {
      return msg + " in " + prettyPrint((Abstract.PrettyPrintableSourceNode) myExpression);
    } else {
      return msg;
    }
  }
}
