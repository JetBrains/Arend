package com.jetbrains.jetpad.vclang.typechecking.error.local;

import com.jetbrains.jetpad.vclang.core.definition.DataDefinition;
import com.jetbrains.jetpad.vclang.error.doc.LineDoc;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.SourceInfoProvider;

import static com.jetbrains.jetpad.vclang.error.doc.DocFactory.*;

public class NonPositiveDataError extends LocalTypeCheckingError {
  public final DataDefinition dataDefinition;
  public final Abstract.Constructor constructor;

  public NonPositiveDataError(DataDefinition dataDefinition, Abstract.Constructor constructor, Abstract.SourceNode cause) {
    super("", cause);
    this.dataDefinition = dataDefinition;
    this.constructor = constructor;
  }

  @Override
  public LineDoc getHeaderDoc(SourceInfoProvider src) {
    return hList(super.getHeaderDoc(src), text(" Non-positive recursive occurrence of data type '"), refDoc(dataDefinition.getAbstractDefinition()), text("' in constructor '"), refDoc(constructor), text("'"));
  }
}
