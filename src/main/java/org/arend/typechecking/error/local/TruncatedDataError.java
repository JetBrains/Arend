package org.arend.typechecking.error.local;

import org.arend.core.definition.DataDefinition;
import org.arend.core.expr.Expression;
import org.arend.ext.prettyprinting.PrettyPrinterConfig;
import org.arend.ext.prettyprinting.doc.Doc;
import org.arend.ext.prettyprinting.doc.LineDoc;
import org.arend.term.concrete.Concrete;

import static org.arend.ext.prettyprinting.doc.DocFactory.*;

public class TruncatedDataError extends TypecheckingError {
  public final DataDefinition dataDef;
  public final Expression expectedType;

  public TruncatedDataError(DataDefinition dataDef, Expression expectedType, Concrete.SourceNode cause) {
    super("", cause);
    this.dataDef = dataDef;
    this.expectedType = expectedType;
  }

  @Override
  public LineDoc getShortHeaderDoc(PrettyPrinterConfig src) {
    return hList(
      text(" Data type '"),
      refDoc(dataDef.getReferable()),
      text("' is truncated to the universe " + dataDef.getSort()));
  }

  @Override
  public Doc getBodyDoc(PrettyPrinterConfig ppConfig) {
    return hang(indent(text("which does not fit in the universe of the eliminator type:")), termDoc(expectedType, ppConfig));
  }

  @Override
  public boolean hasExpressions() {
    return true;
  }
}
