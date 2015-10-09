package com.jetbrains.jetpad.vclang.typechecking.error;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.ResolvedName;

import java.util.List;

import static com.jetbrains.jetpad.vclang.typechecking.error.ArgInferenceError.suffix;

public class InferredArgumentsMismatch extends TypeCheckingError {
  private final int myArgument;
  private final List<Abstract.Expression> myOptions;

  public InferredArgumentsMismatch(ResolvedName resolvedName, int argument, List<Abstract.Expression> options, Abstract.Expression expression, List<String> names) {
    super(resolvedName, null, expression, names);
    myArgument = argument;
    myOptions = options;
  }

  public InferredArgumentsMismatch(int argument, List<Abstract.Expression> options, Abstract.Expression expression, List<String> names) {
    super(null, expression, names);
    myArgument = argument;
    myOptions = options;
  }

  @Override
  public String toString() {
    String msg = printHeader();
    msg += "Cannot infer " + myArgument + suffix(myArgument) + " argument";
    msg += getCause() instanceof Abstract.PrettyPrintableSourceNode ? " to function " + prettyPrint((Abstract.PrettyPrintableSourceNode) getCause()) : "";
    msg += "\nPossible options are:";
    for (Abstract.Expression expression : myOptions) {
      msg += "\n\t" + prettyPrint(expression);
    }
    return msg;
  }
}
