package com.jetbrains.jetpad.vclang.typechecking.error.local;

import com.jetbrains.jetpad.vclang.core.definition.DataDefinition;
import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.error.doc.Doc;
import com.jetbrains.jetpad.vclang.error.doc.LineDoc;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete;
import com.jetbrains.jetpad.vclang.term.prettyprint.PrettyPrinterConfig;

import static com.jetbrains.jetpad.vclang.error.doc.DocFactory.*;

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
}
