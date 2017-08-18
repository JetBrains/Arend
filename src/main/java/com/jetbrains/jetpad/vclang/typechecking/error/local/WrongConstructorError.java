package com.jetbrains.jetpad.vclang.typechecking.error.local;

import com.jetbrains.jetpad.vclang.core.expr.DataCallExpression;
import com.jetbrains.jetpad.vclang.error.doc.LineDoc;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.provider.PrettyPrinterInfoProvider;

import static com.jetbrains.jetpad.vclang.error.doc.DocFactory.*;

public class WrongConstructorError<T> extends LocalTypeCheckingError<T> {
  public final Abstract.Constructor constructor;
  public final DataCallExpression dataCall;

  public WrongConstructorError(Abstract.Constructor constructor, DataCallExpression dataCall, Concrete.SourceNode<T> cause) {
    super("", cause);
    this.constructor = constructor;
    this.dataCall = dataCall;
  }

  @Override
  public LineDoc getHeaderDoc(PrettyPrinterInfoProvider src) {
    return hList(super.getHeaderDoc(src), text(" '"), refDoc(constructor), text("' is not a constructor of data type "), termLine(dataCall));
  }
}
