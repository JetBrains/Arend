package com.jetbrains.jetpad.vclang.typechecking.error.local;

import com.jetbrains.jetpad.vclang.core.definition.DataDefinition;
import com.jetbrains.jetpad.vclang.error.doc.LineDoc;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete;
import com.jetbrains.jetpad.vclang.term.prettyprint.PrettyPrinterConfig;

import static com.jetbrains.jetpad.vclang.error.doc.DocFactory.*;

public class NonPositiveDataError extends TypecheckingError {
  public final DataDefinition dataDefinition;
  public final Concrete.Constructor constructor;

  public NonPositiveDataError(DataDefinition dataDefinition, Concrete.Constructor constructor, Concrete.SourceNode cause) {
    super("", cause);
    this.dataDefinition = dataDefinition;
    this.constructor = constructor;
  }

  @Override
  public LineDoc getShortHeaderDoc(PrettyPrinterConfig src) {
    return hList(text(" Non-positive recursive occurrence of data type '"), refDoc(dataDefinition.getReferable()), text("' in constructor '"), refDoc(constructor.getData()), text("'"));
  }
}
