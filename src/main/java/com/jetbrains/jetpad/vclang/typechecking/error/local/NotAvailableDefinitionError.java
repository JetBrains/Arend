package com.jetbrains.jetpad.vclang.typechecking.error.local;

import com.jetbrains.jetpad.vclang.core.definition.Definition;
import com.jetbrains.jetpad.vclang.error.doc.LineDoc;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.provider.PrettyPrinterInfoProvider;

import static com.jetbrains.jetpad.vclang.error.doc.DocFactory.*;

public class NotAvailableDefinitionError<T> extends LocalTypeCheckingError<T> {
  public final Definition definition;

  public NotAvailableDefinitionError(Definition definition, Concrete.SourceNode<T> cause) {
    super("", cause);
    this.definition = definition;
  }

  @Override
  public LineDoc getHeaderDoc(PrettyPrinterInfoProvider src) {
    return hList(super.getHeaderDoc(src), text(" Definition '"), refDoc(definition.getAbstractDefinition()), text("' is not available in this context"));
  }
}
