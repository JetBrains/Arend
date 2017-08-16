package com.jetbrains.jetpad.vclang.typechecking.error.local;

import com.jetbrains.jetpad.vclang.core.definition.DataDefinition;
import com.jetbrains.jetpad.vclang.error.doc.LineDoc;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.provider.SourceInfoProvider;

import static com.jetbrains.jetpad.vclang.error.doc.DocFactory.*;

public class MissingConstructorError extends LocalTypeCheckingError {
  public final String constructorName;
  public final DataDefinition dataDef;

  public MissingConstructorError(String constructorName, DataDefinition dataDef, Abstract.SourceNode cause) {
    super("", cause);
    this.constructorName = constructorName;
    this.dataDef = dataDef;
  }

  @Override
  public LineDoc getHeaderDoc(SourceInfoProvider src) {
    return hList(super.getHeaderDoc(src), text(" '" + constructorName + "' is not a constructor of data type '"), refDoc(dataDef.getAbstractDefinition()), text("'"));
  }
}
