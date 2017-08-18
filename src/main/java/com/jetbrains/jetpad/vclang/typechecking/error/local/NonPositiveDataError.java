package com.jetbrains.jetpad.vclang.typechecking.error.local;

import com.jetbrains.jetpad.vclang.core.definition.DataDefinition;
import com.jetbrains.jetpad.vclang.error.doc.LineDoc;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.provider.PrettyPrinterInfoProvider;

import static com.jetbrains.jetpad.vclang.error.doc.DocFactory.*;

public class NonPositiveDataError<T> extends LocalTypeCheckingError<T> {
  public final DataDefinition dataDefinition;
  public final Abstract.Constructor constructor;

  public NonPositiveDataError(DataDefinition dataDefinition, Abstract.Constructor constructor, Concrete.SourceNode<T> cause) {
    super("", cause);
    this.dataDefinition = dataDefinition;
    this.constructor = constructor;
  }

  @Override
  public LineDoc getHeaderDoc(PrettyPrinterInfoProvider src) {
    return hList(super.getHeaderDoc(src), text(" Non-positive recursive occurrence of data type '"), refDoc(dataDefinition.getAbstractDefinition()), text("' in constructor '"), refDoc(constructor), text("'"));
  }
}
