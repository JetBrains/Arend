package org.arend.typechecking.error.local;

import org.arend.core.definition.Constructor;
import org.arend.core.expr.DataCallExpression;
import org.arend.ext.error.TypecheckingError;
import org.arend.ext.prettyprinting.PrettyPrinterConfig;
import org.arend.ext.prettyprinting.doc.LineDoc;
import org.arend.naming.reference.GlobalReferable;
import org.arend.term.concrete.Concrete;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.arend.ext.prettyprinting.doc.DocFactory.*;

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

  @NotNull
  @Override
  public Stage getStage() {
    return dataCall == null ? Stage.RESOLVER : Stage.TYPECHECKER;
  }

  @Override
  public boolean hasExpressions() {
    return myConstructorOfData;
  }
}
