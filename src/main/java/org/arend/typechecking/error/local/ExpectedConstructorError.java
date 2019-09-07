package org.arend.typechecking.error.local;

import org.arend.core.expr.DataCallExpression;
import org.arend.error.doc.LineDoc;
import org.arend.naming.reference.GlobalReferable;
import org.arend.term.concrete.Concrete;
import org.arend.term.prettyprint.PrettyPrinterConfig;

import javax.annotation.Nullable;

import static org.arend.error.doc.DocFactory.*;

public class ExpectedConstructorError extends TypecheckingError {
  public final GlobalReferable referable;
  public final DataCallExpression dataCall;

  public ExpectedConstructorError(GlobalReferable referable, @Nullable DataCallExpression dataCall, Concrete.SourceNode cause) {
    super("", cause);
    this.referable = referable;
    this.dataCall = dataCall;
  }

  @Override
  public LineDoc getShortHeaderDoc(PrettyPrinterConfig ppConfig) {
    return hList(text("'"), refDoc(referable), text("' is not a constructor"), dataCall == null ? empty() : hList(text(" of data type "), termLine(dataCall, ppConfig)));
  }

  @Override
  public boolean isTypecheckingError() {
    return dataCall != null;
  }
}
