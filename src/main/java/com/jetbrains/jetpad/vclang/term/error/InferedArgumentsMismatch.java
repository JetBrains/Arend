package com.jetbrains.jetpad.vclang.term.error;

import com.jetbrains.jetpad.vclang.term.expr.Abstract;
import com.jetbrains.jetpad.vclang.term.expr.Expression;

import java.util.List;

import static com.jetbrains.jetpad.vclang.term.error.ArgInferenceError.suffix;

public class InferedArgumentsMismatch extends TypeCheckingError {
  private final int myArgument;
  private final List<Expression> myOptions;

  public InferedArgumentsMismatch(int argument, List<Expression> options, Abstract.Expression expression) {
    super(null, expression);
    myArgument = argument;
    myOptions = options;
  }

  private InferedArgumentsMismatch() {
    myArgument = 0;
    myOptions = null;
  }

  @Override
  public String toString() {
    String msg = "Cannot infer " + myArgument + suffix(myArgument) + " to function " + getExpression() + "\n" +
        "Posible options are:\n";
    for (Expression expression : myOptions) {
      msg += "\t" + expression + "\n";
    }
    return msg;
  }
}
