package com.jetbrains.jetpad.vclang.term.error;

import com.jetbrains.jetpad.vclang.term.Abstract;

import java.util.List;

public class ArgInferenceError extends TypeCheckingError {
  public ArgInferenceError(String message, Abstract.PrettyPrintableSourceNode expression, List<String> names) {
    super(message, expression, names);
  }

  public static String functionArg(int index) {
    return "Cannot infer " + index + suffix(index) + " argument to function";
  }

  public static String lambdaArg(int index) {
    return "Cannot infer " + index + suffix(index) + " argument of lambda";
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
      return getMessage() + " " + getExpression();
    }
  }
}
