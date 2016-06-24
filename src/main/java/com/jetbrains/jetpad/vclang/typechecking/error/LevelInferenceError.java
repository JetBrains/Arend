package com.jetbrains.jetpad.vclang.typechecking.error;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.expr.visitor.PrettyPrintVisitor;

public class LevelInferenceError extends TypeCheckingError {
  public LevelInferenceError(Abstract.Definition definition, Abstract.SourceNode sourceNode) {
    super(definition, "Cannot infer level of '" + PrettyPrintVisitor.prettyPrint(sourceNode, 0) + "'", sourceNode);
    // FIXME[format]
  }

  @Deprecated
  public LevelInferenceError(Abstract.SourceNode sourceNode) {
    super("Cannot infer level of '" + PrettyPrintVisitor.prettyPrint(sourceNode, 0) + "'", sourceNode);
  }
}
