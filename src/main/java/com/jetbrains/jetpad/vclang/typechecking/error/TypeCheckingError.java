package com.jetbrains.jetpad.vclang.typechecking.error;

import com.jetbrains.jetpad.vclang.module.Namespace;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.PrettyPrintable;

import java.util.ArrayList;
import java.util.List;

public class TypeCheckingError extends GeneralError {
  private final Abstract.SourceNode myExpression;
  private final List<String> myNames;

  public TypeCheckingError(Namespace namespace, String message, Abstract.SourceNode expression, List<String> names) {
    super(namespace, message);
    myExpression = expression;
    myNames = names;
  }

  public static List<String> getNames(List<? extends Abstract.Binding> context) {
    List<String> names = new ArrayList<>(context.size());
    for (Abstract.Binding binding : context) {
      names.add(binding.getName() == null ? null : binding.getName().name);
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
  public String printPosition() {
    String msg = getNamespace() == null ? "" : getNamespace().getFullName();
    if (myExpression instanceof Concrete.SourceNode) {
      Concrete.Position position = ((Concrete.SourceNode) myExpression).getPosition();
      if (!msg.isEmpty()) {
        msg += ":";
      }
      msg += position.line + ":" + position.column;
    }
    return msg + ": ";
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
