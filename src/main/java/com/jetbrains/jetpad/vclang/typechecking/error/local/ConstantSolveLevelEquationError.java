package com.jetbrains.jetpad.vclang.typechecking.error.local;

import com.jetbrains.jetpad.vclang.core.context.binding.LevelVariable;
import com.jetbrains.jetpad.vclang.error.doc.LineDoc;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete;
import com.jetbrains.jetpad.vclang.term.provider.PrettyPrinterInfoProvider;

import static com.jetbrains.jetpad.vclang.error.doc.DocFactory.hList;
import static com.jetbrains.jetpad.vclang.error.doc.DocFactory.text;

public class ConstantSolveLevelEquationError extends LocalTypeCheckingError {
  public final LevelVariable variable;

  public ConstantSolveLevelEquationError(LevelVariable variable, Concrete.SourceNode cause) {
    super("", cause);
    this.variable = variable;
  }

  @Override
  public LineDoc getHeaderDoc(PrettyPrinterInfoProvider src) {
    return hList(super.getHeaderDoc(src), text(" Cannot solve equation " + variable + " <= constant"));
  }
}
