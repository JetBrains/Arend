package com.jetbrains.jetpad.vclang.typechecking.error.local;

import com.jetbrains.jetpad.vclang.core.definition.Definition;
import com.jetbrains.jetpad.vclang.error.doc.LineDoc;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete;
import com.jetbrains.jetpad.vclang.term.prettyprint.PrettyPrinterConfig;

import static com.jetbrains.jetpad.vclang.error.doc.DocFactory.*;

public class NotAvailableDefinitionError extends TypecheckingError {
  public final Definition definition;

  public NotAvailableDefinitionError(Definition definition, Concrete.SourceNode cause) {
    super("", cause);
    this.definition = definition;
  }

  @Override
  public LineDoc getHeaderDoc(PrettyPrinterConfig src) {
    return hList(super.getHeaderDoc(src), text(" Definition '"), refDoc(definition.getReferable()), text("' is not available in this context"));
  }
}
