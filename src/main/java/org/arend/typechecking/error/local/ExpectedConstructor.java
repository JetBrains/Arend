package org.arend.typechecking.error.local;

import org.arend.core.expr.DataCallExpression;
import org.arend.error.doc.LineDoc;
import org.arend.naming.reference.Referable;
import org.arend.term.concrete.Concrete;
import org.arend.term.prettyprint.PrettyPrinterConfig;

import static org.arend.error.doc.DocFactory.*;

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
