package com.jetbrains.jetpad.vclang.typechecking.error.local;

import com.jetbrains.jetpad.vclang.core.expr.DataCallExpression;
import com.jetbrains.jetpad.vclang.error.doc.LineDoc;
import com.jetbrains.jetpad.vclang.naming.reference.Referable;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete;
import com.jetbrains.jetpad.vclang.term.prettyprint.PrettyPrinterConfig;

import static com.jetbrains.jetpad.vclang.error.doc.DocFactory.*;

public class ExpectedConstructor extends TypecheckingError {
  public final Referable referable;
  public final DataCallExpression dataCall;

  public ExpectedConstructor(Referable referable, DataCallExpression dataCall, Concrete.SourceNode cause) {
    super("", cause);
    this.referable = referable;
    this.dataCall = dataCall;
  }

  @Override
  public LineDoc getShortHeaderDoc(PrettyPrinterConfig ppConfig) {
    return hList(text("'"), refDoc(referable), text("' is not a constructor of data type "), termLine(dataCall, ppConfig));
  }
}
