package org.arend.typechecking.error.local;

import org.arend.core.definition.DataDefinition;
import org.arend.ext.prettyprinting.PrettyPrinterConfig;
import org.arend.ext.prettyprinting.doc.LineDoc;
import org.arend.term.concrete.Concrete;

import static org.arend.ext.prettyprinting.doc.DocFactory.*;

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
