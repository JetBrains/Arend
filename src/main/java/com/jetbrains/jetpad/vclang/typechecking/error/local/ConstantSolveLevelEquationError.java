package com.jetbrains.jetpad.vclang.typechecking.error.local;

import com.jetbrains.jetpad.vclang.core.context.binding.LevelVariable;
import com.jetbrains.jetpad.vclang.core.context.binding.inference.InferenceLevelVariable;
import com.jetbrains.jetpad.vclang.error.SourceInfo;
import com.jetbrains.jetpad.vclang.error.doc.Doc;
import com.jetbrains.jetpad.vclang.error.doc.LineDoc;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete;
import com.jetbrains.jetpad.vclang.term.provider.PrettyPrinterInfoProvider;

import static com.jetbrains.jetpad.vclang.error.doc.DocFactory.*;

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

  @Override
  public Doc getBodyDoc(PrettyPrinterInfoProvider src) {
    if (variable instanceof InferenceLevelVariable) {
      Concrete.SourceNode sourceNode = ((InferenceLevelVariable) variable).getSourceNode();
      if (sourceNode.getData() != getCause()) {
        String position = sourceNode.getData() instanceof SourceInfo ? ((SourceInfo) sourceNode.getData()).positionTextRepresentation() : null;
        return hang(hList(text("where " + variable.toString() + " is defined"), position != null ? text(" at " + position) : empty(), text(" for")), ppDoc(sourceNode, src));
      }
    }
    return nullDoc();
  }
}
