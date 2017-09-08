package com.jetbrains.jetpad.vclang.typechecking.error.local;

import com.jetbrains.jetpad.vclang.core.definition.DataDefinition;
import com.jetbrains.jetpad.vclang.error.doc.LineDoc;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.provider.PrettyPrinterInfoProvider;

import static com.jetbrains.jetpad.vclang.error.doc.DocFactory.*;

public class NonPositiveDataError extends LocalTypeCheckingError {
  public final DataDefinition dataDefinition;
  public final Concrete.Constructor constructor;

  public NonPositiveDataError(DataDefinition dataDefinition, Concrete.Constructor constructor, Concrete.SourceNode cause) {
    super("", cause);
    this.dataDefinition = dataDefinition;
    this.constructor = constructor;
  }

  @Override
  public LineDoc getHeaderDoc(PrettyPrinterInfoProvider src) {
    return hList(super.getHeaderDoc(src), text(" Non-positive recursive occurrence of data type '"), refDoc(dataDefinition.getReferable()), text("' in constructor '"), refDoc(constructor.getReferable()), text("'"));
  }
}
