package com.jetbrains.jetpad.vclang.typechecking.error;

import com.jetbrains.jetpad.vclang.module.Namespace;
import com.jetbrains.jetpad.vclang.term.Abstract;

import java.util.List;

import static com.jetbrains.jetpad.vclang.typechecking.error.ArgInferenceError.suffix;

public class InferredArgumentsMismatch extends TypeCheckingError {
  private final int myArgument;
  private final List<Abstract.Expression> myOptions;

  public InferredArgumentsMismatch(Namespace namespace, int argument, List<Abstract.Expression> options, Abstract.Expression expression, List<String> names) {
    super(namespace, null, expression, names);
    myArgument = argument;
    myOptions = options;
  }

  @Override
  public String toString() {
    String msg = printPosition();
    msg += "Cannot infer " + myArgument + suffix(myArgument) + " argument";
    msg += getCause() instanceof Abstract.PrettyPrintableSourceNode ? " to function " + prettyPrint((Abstract.PrettyPrintableSourceNode) getCause()) : "";
    msg += "\nPossible options are:";
    for (Abstract.Expression expression : myOptions) {
      msg += "\n\t" + prettyPrint(expression);
    }
    return msg;
  }
}
