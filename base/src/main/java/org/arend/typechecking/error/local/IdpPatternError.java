package org.arend.typechecking.error.local;

import org.arend.core.expr.DataCallExpression;
import org.arend.ext.error.TypecheckingError;
import org.arend.ext.prettifier.ExpressionPrettifier;
import org.arend.ext.prettyprinting.PrettyPrinterConfig;
import org.arend.ext.prettyprinting.doc.Doc;
import org.arend.term.concrete.Concrete;
import org.jetbrains.annotations.NotNull;

import static org.arend.ext.prettyprinting.doc.DocFactory.*;

public class IdpPatternError extends TypecheckingError {
  private final ExpressionPrettifier myPrettifier;
  public final DataCallExpression expectedType;

  public IdpPatternError(ExpressionPrettifier prettifier, String message, DataCallExpression expectedType, @NotNull Concrete.SourceNode cause) {
    super(message, cause);
    myPrettifier = prettifier;
    this.expectedType = expectedType;
  }

  public static String noVariable() {
    return "One of the sides in the expected type should be a variable";
  }

  public static String variable(String var) {
    return "Variable '" + var + "' can appear in the expected type only once";
  }

  public static String typeMismatch() {
    return "The type of the variable does not match the type of the equality";
  }

  public static String subst(String substVar, String paramVar, String freeVar) {
    return "Cannot substitute variable '" + substVar + "' into parameter '" + paramVar + "' since the corresponding expression contains free variable '" + freeVar + "' which is not in the scope of the parameter";
  }

  public static String noParameter() {
    return "One of the sides in the expected type should be a parameter";
  }

  @Override
  public Doc getBodyDoc(PrettyPrinterConfig ppConfig) {
    return expectedType == null ? nullDoc() : hang(text("Expected type:"), termDoc(expectedType, myPrettifier, ppConfig));
  }

  @Override
  public boolean hasExpressions() {
    return expectedType != null;
  }
}
