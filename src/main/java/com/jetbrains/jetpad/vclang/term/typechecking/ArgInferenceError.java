package com.jetbrains.jetpad.vclang.term.typechecking;

import com.jetbrains.jetpad.vclang.term.expr.Abstract;

public class ArgInferenceError extends TypeCheckingError {
  private final int myIndex;

  public ArgInferenceError(String message, Abstract.Expression expression, int index) {
    super(message, expression);
    myIndex = index;
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
    String message = "Cannot infer " + myIndex + suffix(myIndex) + " argument " + getMessage();
    if (getExpression() == null) {
      return message;
    } else {
      return message + " " + getExpression();
    }
  }
}
