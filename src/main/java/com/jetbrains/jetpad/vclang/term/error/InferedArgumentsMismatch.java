package com.jetbrains.jetpad.vclang.term.error;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.expr.Expression;

import java.util.List;

import static com.jetbrains.jetpad.vclang.term.error.ArgInferenceError.suffix;

public class InferedArgumentsMismatch extends TypeCheckingError {
  private final int myArgument;
  private final List<Expression> myOptions;

  public InferedArgumentsMismatch(int argument, List<Expression> options, Abstract.Expression expression, List<String> names) {
    super(null, expression, names);
    myArgument = argument;
    myOptions = options;
  }

  @Override
  public String toString() {
    String msg = "Cannot infer " + myArgument + suffix(myArgument) + " argument to function " + getExpression() + "\n" +
        "Posible options are:\n";
    for (Expression expression : myOptions) {
      msg += "\t" + expression + "\n";
    }
    return msg;
  }
}
