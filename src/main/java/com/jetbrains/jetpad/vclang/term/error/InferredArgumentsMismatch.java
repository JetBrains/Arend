package com.jetbrains.jetpad.vclang.term.error;

import com.jetbrains.jetpad.vclang.term.Abstract;

import java.util.List;

import static com.jetbrains.jetpad.vclang.term.error.ArgInferenceError.suffix;

public class InferredArgumentsMismatch extends TypeCheckingError {
  private final int myArgument;
  private final List<Abstract.Expression> myOptions;

  public InferredArgumentsMismatch(int argument, List<Abstract.Expression> options, Abstract.Expression expression, List<String> names) {
    super(null, expression, names);
    myArgument = argument;
    myOptions = options;
  }

  @Override
  public String toString() {
    String msg = "Cannot infer " + myArgument + suffix(myArgument) + " argument to function " + prettyPrint(getExpression()) + "\n" +
        "Posible options are:";
    for (Abstract.Expression expression : myOptions) {
      msg += "\n\t" + prettyPrint(expression);
    }
    return msg;
  }
}
