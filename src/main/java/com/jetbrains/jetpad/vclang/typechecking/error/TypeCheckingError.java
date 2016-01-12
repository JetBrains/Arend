package com.jetbrains.jetpad.vclang.typechecking.error;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.PrettyPrintable;
import com.jetbrains.jetpad.vclang.term.definition.ResolvedName;
import com.jetbrains.jetpad.vclang.term.expr.param.Binding;

import java.util.ArrayList;
import java.util.List;

public class TypeCheckingError extends GeneralError {
  private final Abstract.SourceNode myExpression;
  private final List<String> myNames;

  public TypeCheckingError(ResolvedName resolvedName, String message, Abstract.SourceNode expression, List<String> names) {
    super(resolvedName, message);
    myExpression = expression;
    myNames = names;
  }

  public TypeCheckingError(String message, Abstract.SourceNode expression, List<String> names) {
    super(message);
    myExpression = expression;
    myNames = names;
  }

  protected List<String> getNames() {
    return myNames;
  }

  public static List<String> getNames(List<? extends Binding> context) {
    List<String> names = new ArrayList<>(context.size());
    for (Binding binding : context) {
      names.add(binding.getName() == null ? null : binding.getName());
    }
    return names;
  }

  @Override
  public Abstract.SourceNode getCause() {
    return myExpression;
  }

  protected String prettyPrint(PrettyPrintable expression) {
    StringBuilder builder = new StringBuilder();
    expression.prettyPrint(builder, myNames, Abstract.Expression.PREC);
    return builder.toString();
  }

  @Override
  public String printHeader() {
    String msg = super.printHeader();
    if (myExpression instanceof Concrete.SourceNode) {
      Concrete.Position position = ((Concrete.SourceNode) myExpression).getPosition();
      if (position != null) {
        msg += position.line + ":" + position.column + ": ";
      }
    }
    return msg;
  }

  @Override
  public String toString() {
    String msg = super.toString();
    if (myExpression instanceof Abstract.PrettyPrintableSourceNode) {
      return msg + " in " + prettyPrint((Abstract.PrettyPrintableSourceNode) myExpression);
    } else {
      return msg;
    }
  }
}
