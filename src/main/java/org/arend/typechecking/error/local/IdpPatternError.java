package org.arend.typechecking.error.local;

import org.arend.core.expr.DataCallExpression;
import org.arend.error.doc.Doc;
import org.arend.term.concrete.Concrete;
import org.arend.term.prettyprint.PrettyPrinterConfig;

import javax.annotation.Nonnull;

import static org.arend.error.doc.DocFactory.*;

public class IdpPatternError extends TypecheckingError {
  public final DataCallExpression expectedType;

  public IdpPatternError(String message, DataCallExpression expectedType, @Nonnull Concrete.SourceNode cause) {
    super(message, cause);
    this.expectedType = expectedType;
  }

  public static String noVariable() {
    return "One of the sides in the expected type should be a variable";
  }

  public static String variable(String var) {
    return "Variable '" + var + "' can appear in the expected type only once";
  }

  public static String subst(String substVar, String paramVar, String freeVar) {
    return "Cannot substitute variable '" + substVar + "' into parameter '" + paramVar + "' since the corresponding expression contains free variable '" + freeVar + "' which is not in the scope of the parameter";
  }

  public static String noParameter() {
    return "One of the sides in the expected type should be a variable";
  }

  @Override
  public Doc getBodyDoc(PrettyPrinterConfig ppConfig) {
    return expectedType == null ? nullDoc() : hang(text("Expected type:"), termDoc(expectedType, ppConfig));
  }

  @Override
  public boolean isShort() {
    return false;
  }
}
