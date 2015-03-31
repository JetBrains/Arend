package com.jetbrains.jetpad.vclang.term.error;

import com.jetbrains.jetpad.vclang.term.expr.Abstract;

public class TypeInferenceError extends TypeCheckingError {
  public TypeInferenceError(Abstract.Expression expression) {
      super(null, expression);
  }

  @Override
  public String toString() {
    String message = "Cannot infer type";
    if (getExpression() == null) {
      return message;
    } else {
      return message + " of " + getExpression();
    }
  }
}
