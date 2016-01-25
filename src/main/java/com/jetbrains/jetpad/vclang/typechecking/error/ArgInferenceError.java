package com.jetbrains.jetpad.vclang.typechecking.error;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.PrettyPrintable;
import com.jetbrains.jetpad.vclang.term.definition.Name;
import com.jetbrains.jetpad.vclang.term.definition.ResolvedName;

import java.util.List;

public class ArgInferenceError extends TypeCheckingError {
  private final PrettyPrintable myWhere;

  public ArgInferenceError(ResolvedName resolvedName, String message, Abstract.SourceNode expression, PrettyPrintable where) {
    super(resolvedName, message, expression);
    myWhere = where;
  }

  public ArgInferenceError(String message, Abstract.SourceNode expression, PrettyPrintable where) {
    super(message, expression);
    myWhere = where;
  }

  public static String functionArg(int index) {
    return "Cannot infer " + index + suffix(index) + " argument to function";
  }

  public static String typeOfFunctionArg(int index) {
    return "Cannot infer type of " + index + suffix(index) + " argument of function";
  }

  public static String lambdaArg(int index) {
    return "Cannot infer type of the " + index + suffix(index) + " argument of lambda";
  }

  public static String parameter(int index) {
    return "Cannot infer " + index + suffix(index) + " parameter to constructor";
  }

  public static String expression() {
    return "Cannot infer an expression";
  }

  public static String type() {
    return "Cannot infer type of expression";
  }

  public static String suffix(int n) {
    switch (n) {
      case 1: return "st";
      case 2: return "nd";
      case 3: return "rd";
      default: return "th";
    }
  }

  @Override
  public String toString() {
    String msg = printHeader() + getMessage();
    if (getCause() == null) {
      return msg;
    } else {
      if (myWhere != null) {
        msg += " " + prettyPrint(myWhere);
      }
      return msg;
    }
  }

  public static class StringPrettyPrintable implements PrettyPrintable {
    private final String myString;

    public StringPrettyPrintable(String string) {
      myString = string;
    }

    public StringPrettyPrintable(Name name) {
      myString = name.getPrefixName();
    }

    @Override
    public void prettyPrint(StringBuilder builder, List<String> names, byte prec) {
      builder.append(myString);
    }
  }
}
