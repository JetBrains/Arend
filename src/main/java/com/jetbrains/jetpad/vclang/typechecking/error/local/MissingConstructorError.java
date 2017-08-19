package com.jetbrains.jetpad.vclang.typechecking.error.local;

import com.jetbrains.jetpad.vclang.core.definition.DataDefinition;
import com.jetbrains.jetpad.vclang.error.doc.LineDoc;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.provider.PrettyPrinterInfoProvider;

import static com.jetbrains.jetpad.vclang.error.doc.DocFactory.*;

public class MissingConstructorError<T> extends LocalTypeCheckingError<T> {
  public final String constructorName;
  public final DataDefinition dataDef;

  public MissingConstructorError(String constructorName, DataDefinition dataDef, Concrete.SourceNode<T> cause) {
    super("", cause);
    this.constructorName = constructorName;
    this.dataDef = dataDef;
  }

  @Override
  public LineDoc getHeaderDoc(PrettyPrinterInfoProvider src) {
    return hList(super.getHeaderDoc(src), text(" '" + constructorName + "' is not a constructor of data type '"), refDoc(dataDef.getConcreteDefinition()), text("'"));
  }
}
