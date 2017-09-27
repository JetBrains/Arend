package com.jetbrains.jetpad.vclang.typechecking.error.local;

import com.jetbrains.jetpad.vclang.core.context.binding.LevelVariable;
import com.jetbrains.jetpad.vclang.core.context.binding.inference.InferenceLevelVariable;
import com.jetbrains.jetpad.vclang.error.doc.Doc;
import com.jetbrains.jetpad.vclang.error.doc.LineDoc;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.SourceInfoProvider;

import static com.jetbrains.jetpad.vclang.error.doc.DocFactory.*;

public class ConstantSolveLevelEquationError extends LocalTypeCheckingError {
  public final LevelVariable variable;

  public ConstantSolveLevelEquationError(LevelVariable variable, Abstract.SourceNode cause) {
    super("", cause);
    this.variable = variable;
  }

  @Override
  public LineDoc getHeaderDoc(SourceInfoProvider src) {
    return hList(super.getHeaderDoc(src), text(" Cannot solve equation " + variable + " <= constant"));
  }

  @Override
  public Doc getBodyDoc(SourceInfoProvider src) {
    if (variable instanceof InferenceLevelVariable) {
      Abstract.SourceNode sourceNode = ((InferenceLevelVariable) variable).getSourceNode();
      if (sourceNode != getCause()) {
        String position = src.positionOf(sourceNode);
        return hang(hList(text("where " + variable.toString() + " is defined"), position != null ? text(" at " + position) : empty(), text(" for")), sourceNodeDoc(sourceNode));
      }
    }
    return nullDoc();
  }
}
