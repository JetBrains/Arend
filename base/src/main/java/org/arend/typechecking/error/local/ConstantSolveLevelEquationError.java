package org.arend.typechecking.error.local;

import org.arend.core.context.binding.LevelVariable;
import org.arend.core.context.binding.inference.InferenceLevelVariable;
import org.arend.ext.error.SourceInfo;
import org.arend.ext.error.TypecheckingError;
import org.arend.ext.prettyprinting.PrettyPrinterConfig;
import org.arend.ext.prettyprinting.doc.Doc;
import org.arend.term.concrete.Concrete;

import static org.arend.ext.prettyprinting.doc.DocFactory.*;

public class ConstantSolveLevelEquationError extends TypecheckingError {
  public final LevelVariable variable;

  public ConstantSolveLevelEquationError(LevelVariable variable, Concrete.SourceNode cause) {
    super("Cannot solve equation " + variable + " <= constant", cause);
    this.variable = variable;
  }

  @Override
  public Doc getBodyDoc(PrettyPrinterConfig src) {
    if (variable instanceof InferenceLevelVariable) {
      Concrete.SourceNode sourceNode = ((InferenceLevelVariable) variable).getSourceNode();
      if (sourceNode.getData() != getCause()) {
        SourceInfo sourceInfo = SourceInfo.getSourceInfo(sourceNode.getData());
        String position = sourceInfo != null ? sourceInfo.positionTextRepresentation() : null;
        return hang(hList(text("where " + variable + " is defined"), position != null ? text(" at " + position) : empty(), text(" for")), ppDoc(sourceNode, src));
      }
    }
    return nullDoc();
  }

  @Override
  public boolean hasExpressions() {
    return variable instanceof InferenceLevelVariable && ((InferenceLevelVariable) variable).getSourceNode().getData() != getCause();
  }
}
