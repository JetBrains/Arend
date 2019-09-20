package org.arend.typechecking.error.local;

import org.arend.core.definition.Constructor;
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
  private final boolean myConstructorOfData;

  public ExpectedConstructorError(GlobalReferable referable, @Nullable DataCallExpression dataCall, Concrete.SourceNode cause) {
    super("", cause);
    this.referable = referable;
    this.dataCall = dataCall;

    boolean constructorOfData = false;
    if (dataCall != null) {
      for (Constructor constructor : dataCall.getDefinition().getConstructors()) {
        if (constructor.getReferable() == referable) {
          constructorOfData = true;
          break;
        }
      }
    }

    myConstructorOfData = constructorOfData;
  }

  @Override
  public LineDoc getShortHeaderDoc(PrettyPrinterConfig ppConfig) {
    return hList(text("'"), refDoc(referable), text("' is not a constructor"), dataCall == null ? empty() : hList(text(" of data type "), myConstructorOfData ? termLine(dataCall, ppConfig) : refDoc(dataCall.getDefinition().getReferable())));
  }

  @Override
  public Stage getStage() {
    return dataCall == null ? Stage.RESOLVER : Stage.TYPECHECKER;
  }

  @Override
  public boolean isShort() {
    return !myConstructorOfData;
  }
}
