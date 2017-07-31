package com.jetbrains.jetpad.vclang.typechecking.error.local;

import com.jetbrains.jetpad.vclang.core.expr.DataCallExpression;
import com.jetbrains.jetpad.vclang.error.doc.LineDoc;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.SourceInfoProvider;

import static com.jetbrains.jetpad.vclang.error.doc.DocFactory.*;

public class WrongConstructorError extends LocalTypeCheckingError {
  public final Abstract.Constructor constructor;
  public final DataCallExpression dataCall;

  public WrongConstructorError(Abstract.Constructor constructor, DataCallExpression dataCall, Abstract.SourceNode cause) {
    super("", cause);
    this.constructor = constructor;
    this.dataCall = dataCall;
  }

  @Override
  public LineDoc getHeaderDoc(SourceInfoProvider src) {
    return hList(super.getHeaderDoc(src), text(" '"), refDoc(constructor), text("' is not a constructor of data type "), termLine(dataCall));
  }
}
