package com.jetbrains.jetpad.vclang.typechecking.error;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.context.binding.LevelInferenceBinding;
import com.jetbrains.jetpad.vclang.term.expr.visitor.PrettyPrintVisitor;

public class LevelInferenceError extends TypeCheckingError {
  public LevelInferenceError(LevelInferenceBinding levelVar, Abstract.Definition definition, Abstract.SourceNode sourceNode) {
    super(definition, "Cannot infer level " + levelVar + " of '" + PrettyPrintVisitor.prettyPrint(sourceNode, 0) + "'", sourceNode);
    // FIXME[format]
  }

  @Deprecated
  public LevelInferenceError(LevelInferenceBinding levelVar, Abstract.SourceNode sourceNode) {
    super("Cannot infer level " + levelVar + " of '" + PrettyPrintVisitor.prettyPrint(sourceNode, 0) + "'", sourceNode);
  }
}
