package com.jetbrains.jetpad.vclang.typechecking.error;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.expr.visitor.PrettyPrintVisitor;

import java.util.ArrayList;

public class LevelInferenceError extends TypeCheckingError {
  public LevelInferenceError(Abstract.SourceNode sourceNode) {
    super("Cannot infer level of ", sourceNode);
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append(printHeader()).append(getMessage());
    new PrettyPrintVisitor(builder, new ArrayList<String>(), 0).prettyPrint(getCause(), Abstract.Expression.PREC);
    return builder.toString();
  }
}
