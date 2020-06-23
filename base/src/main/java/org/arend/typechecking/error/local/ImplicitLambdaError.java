package org.arend.typechecking.error.local;

import org.arend.ext.error.TypecheckingError;
import org.arend.ext.prettyprinting.PrettyPrinterConfig;
import org.arend.ext.prettyprinting.doc.LineDoc;
import org.arend.naming.reference.Referable;
import org.arend.term.concrete.Concrete;
import org.arend.typechecking.error.local.inference.ArgInferenceError;
import org.jetbrains.annotations.NotNull;

import static org.arend.ext.prettyprinting.doc.DocFactory.*;

public class ImplicitLambdaError extends TypecheckingError {
  public final Referable parameter;
  public final int index;

  public ImplicitLambdaError(Referable parameter, int index, @NotNull Concrete.Parameter cause) {
    super("", cause);
    this.parameter = parameter;
    this.index = index;
  }

  @Override
  public LineDoc getShortHeaderDoc(PrettyPrinterConfig ppConfig) {
    String msg = " is implicit, but the corresponding parameter of the expected type is not";
    if (parameter != null) {
      return hList(text("Parameter '"), refDoc(parameter), text("'" + msg));
    }
    if (index >= 0) {
      return text(ArgInferenceError.ordinal(index + 1) + " parameter" + msg);
    }
    return text("Parameter" + msg);
  }
}
