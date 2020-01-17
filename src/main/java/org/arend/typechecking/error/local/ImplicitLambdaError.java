package org.arend.typechecking.error.local;

import org.arend.ext.prettyprinting.PrettyPrinterConfig;
import org.arend.ext.prettyprinting.doc.LineDoc;
import org.arend.naming.reference.Referable;
import org.arend.term.concrete.Concrete;

import javax.annotation.Nonnull;

import static org.arend.ext.prettyprinting.doc.DocFactory.*;

public class ImplicitLambdaError extends TypecheckingError {
  public final Referable parameter;

  public ImplicitLambdaError(Referable parameter, @Nonnull Concrete.LamExpression cause) {
    super("", cause);
    this.parameter = parameter;
  }

  @Override
  public LineDoc getShortHeaderDoc(PrettyPrinterConfig ppConfig) {
    return hList(text("Parameter '"), refDoc(parameter), text("' of the lambda is implicit, but the corresponding parameter of the expected type is not"));
  }
}
