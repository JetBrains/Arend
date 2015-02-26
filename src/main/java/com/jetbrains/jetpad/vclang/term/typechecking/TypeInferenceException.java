package com.jetbrains.jetpad.vclang.term.typechecking;

import com.jetbrains.jetpad.vclang.term.expr.Abstract;

public class TypeInferenceException extends TypeCheckingException {
  public TypeInferenceException(Abstract.Expression expression) {
      super(expression);
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
