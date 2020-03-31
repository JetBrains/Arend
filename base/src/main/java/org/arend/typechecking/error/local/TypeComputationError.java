package org.arend.typechecking.error.local;

import org.arend.core.expr.Expression;
import org.arend.ext.concrete.ConcreteSourceNode;
import org.arend.ext.error.TypecheckingError;
import org.arend.ext.prettyprinting.PrettyPrinterConfig;
import org.arend.ext.prettyprinting.doc.Doc;
import org.arend.ext.prettyprinting.doc.DocFactory;
import org.arend.naming.reference.Referable;
import org.jetbrains.annotations.Nullable;

public class TypeComputationError extends TypecheckingError {
  public final Referable referable;
  public final Expression expression;

  public TypeComputationError(Referable referable, Expression expression, @Nullable ConcreteSourceNode cause) {
    super("Cannot compute the type" + (referable == null ? "" : " for '" + referable.getRefName() + "'"), cause);
    this.referable = referable;
    this.expression = expression;
  }

  @Override
  public Doc getBodyDoc(PrettyPrinterConfig ppConfig) {
    return DocFactory.hang(DocFactory.text("Expression:"), DocFactory.termDoc(expression, ppConfig));
  }

  @Override
  public boolean hasExpressions() {
    return true;
  }
}
