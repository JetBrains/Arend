package org.arend.typechecking.error.local;

import org.arend.core.expr.Expression;
import org.arend.ext.concrete.ConcreteSourceNode;
import org.arend.ext.error.GeneralError;
import org.arend.ext.error.LocalError;
import org.arend.ext.prettyprinting.PrettyPrinterConfig;
import org.arend.ext.prettyprinting.doc.Doc;
import org.arend.ext.prettyprinting.doc.DocFactory;
import org.arend.ext.prettyprinting.doc.LineDoc;
import org.jetbrains.annotations.NotNull;

public class CoreErrorWrapper extends LocalError {
  public final GeneralError error;
  public final Expression causeExpr;

  private CoreErrorWrapper(GeneralError error, Expression causeExpr) {
    super(error.level, error.message);
    this.error = error;
    this.causeExpr = causeExpr;
  }

  public static GeneralError make(GeneralError error, Expression causeExpr) {
    return causeExpr == null ? error : new CoreErrorWrapper(error, causeExpr);
  }

  @Override
  public ConcreteSourceNode getCauseSourceNode() {
    return error.getCauseSourceNode();
  }

  @Override
  public Object getCause() {
    Object cause = error.getCause();
    return cause != null ? cause : definition;
  }

  @Override
  public @NotNull Stage getStage() {
    return Stage.TYPECHECKER;
  }

  @Override
  public Doc getCauseDoc(PrettyPrinterConfig ppConfig) {
    return DocFactory.termDoc(causeExpr, ppConfig);
  }

  @Override
  public LineDoc getShortHeaderDoc(PrettyPrinterConfig ppConfig) {
    return error.getShortHeaderDoc(ppConfig);
  }

  @Override
  public Doc getBodyDoc(PrettyPrinterConfig ppConfig) {
    return error.getBodyDoc(ppConfig);
  }
}
