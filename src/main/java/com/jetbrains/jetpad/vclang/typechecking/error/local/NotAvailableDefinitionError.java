package com.jetbrains.jetpad.vclang.typechecking.error.local;

import com.jetbrains.jetpad.vclang.core.definition.Definition;
import com.jetbrains.jetpad.vclang.error.doc.LineDoc;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.provider.SourceInfoProvider;

import static com.jetbrains.jetpad.vclang.error.doc.DocFactory.*;

public class NotAvailableDefinitionError extends LocalTypeCheckingError {
  public final Definition definition;

  public NotAvailableDefinitionError(Definition definition, Abstract.SourceNode cause) {
    super("", cause);
    this.definition = definition;
  }

  @Override
  public LineDoc getHeaderDoc(SourceInfoProvider src) {
    return hList(super.getHeaderDoc(src), text(" Definition '"), refDoc(definition.getAbstractDefinition()), text("' is not available in this context"));
  }
}
