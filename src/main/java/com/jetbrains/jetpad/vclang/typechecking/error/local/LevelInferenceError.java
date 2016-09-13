package com.jetbrains.jetpad.vclang.typechecking.error.local;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.context.binding.inference.LevelInferenceVariable;
import com.jetbrains.jetpad.vclang.term.expr.visitor.PrettyPrintVisitor;

public class LevelInferenceError extends LocalTypeCheckingError {
  public com.jetbrains.jetpad.vclang.term.expr.sort.Level[] candidates = new com.jetbrains.jetpad.vclang.term.expr.sort.Level[0];

  public LevelInferenceError(LevelInferenceVariable levelVar, Abstract.SourceNode sourceNode) {
    super("Cannot infer level " + levelVar + " of '" + PrettyPrintVisitor.prettyPrint(sourceNode, 0) + "'", sourceNode);
  }

  public LevelInferenceError(LevelInferenceVariable levelVar, Abstract.SourceNode sourceNode, com.jetbrains.jetpad.vclang.term.expr.sort.Level... candidates) {
    super("Cannot infer level " + levelVar + " of '" + PrettyPrintVisitor.prettyPrint(sourceNode, 0) + "'", sourceNode);
    this.candidates = candidates;
  }
}
