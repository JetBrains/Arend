package com.jetbrains.jetpad.vclang.term.error;

import com.jetbrains.jetpad.vclang.term.Abstract;

import java.util.List;

public class ArgInferenceError extends TypeCheckingError {
  private final Abstract.PrettyPrintableSourceNode myWhere;

  public ArgInferenceError(String message, Abstract.PrettyPrintableSourceNode expression, List<String> names, Abstract.PrettyPrintableSourceNode where) {
    super(message, expression, names);
    myWhere = where;
  }

  public static String functionArg(int index) {
    return "Cannot infer " + index + suffix(index) + " argument to function";
  }

  public static String lambdaArg(int index) {
    return "Cannot infer type of the " + index + suffix(index) + " argument of lambda";
  }

  public static String parameter(int index) {
    return "Cannot infer " + index + suffix(index) + " parameter to data type";
  }

  public static String expression() {
    return "Cannot infer an expression";
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
    if (getExpression() == null) {
      return getMessage();
    } else {
      String msg = getMessage();
      if (myWhere != null) {
        msg += " " + prettyPrint(myWhere);
      }
      return msg;
    }
  }
}
