package org.arend.typechecking.error.local;

import org.arend.core.context.binding.LevelVariable;
import org.arend.core.context.binding.inference.InferenceLevelVariable;
import org.arend.error.SourceInfo;
import org.arend.error.doc.Doc;
import org.arend.error.doc.LineDoc;
import org.arend.term.concrete.Concrete;
import org.arend.term.prettyprint.PrettyPrinterConfig;

import static org.arend.error.doc.DocFactory.*;

public class ConstantSolveLevelEquationError extends TypecheckingError {
  public final LevelVariable variable;

  public ConstantSolveLevelEquationError(LevelVariable variable, Concrete.SourceNode cause) {
    super("", cause);
    this.variable = variable;
  }

  @Override
  public LineDoc getShortHeaderDoc(PrettyPrinterConfig src) {
    return text("Cannot solve equation " + variable + " <= constant");
  }

  @Override
  public Doc getBodyDoc(PrettyPrinterConfig src) {
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
