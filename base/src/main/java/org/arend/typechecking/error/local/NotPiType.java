package org.arend.typechecking.error.local;

import org.arend.core.expr.Expression;
import org.arend.ext.error.TypecheckingError;
import org.arend.ext.prettifier.ExpressionPrettifier;
import org.arend.ext.prettyprinting.PrettyPrinterConfig;
import org.arend.ext.prettyprinting.doc.Doc;
import org.arend.term.concrete.Concrete;

import static org.arend.ext.prettyprinting.doc.DocFactory.*;

public class NotPiType extends TypecheckingError {
  private final ExpressionPrettifier myPrettifier;
  public final Expression argument;
  public final Expression type;

  public NotPiType(ExpressionPrettifier prettifier, Expression argument, Expression type, Concrete.SourceNode cause) {
    super("Expression is applied to an argument, but does not have a function type", cause);
    myPrettifier = prettifier;
    this.argument = argument;
    this.type = type;
  }

  @Override
  public Doc getBodyDoc(PrettyPrinterConfig ppConfig) {
    return vList(
      hang(text("Argument:"), termDoc(argument, myPrettifier, ppConfig)),
      hang(text("Type:"), termDoc(type, myPrettifier, ppConfig))
    );
  }

  @Override
  public boolean hasExpressions() {
    return true;
  }
}
