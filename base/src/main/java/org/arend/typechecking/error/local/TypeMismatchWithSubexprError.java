package org.arend.typechecking.error.local;

import org.arend.core.expr.visitor.CompareVisitor;
import org.arend.ext.concrete.ConcreteSourceNode;
import org.arend.ext.error.TypecheckingError;
import org.arend.ext.prettyprinting.PrettyPrinterConfig;
import org.arend.ext.prettyprinting.doc.Doc;
import org.arend.term.prettyprint.TermWithSubtermDoc;

import static org.arend.ext.prettyprinting.doc.DocFactory.*;

public class TypeMismatchWithSubexprError extends TypecheckingError {
  public final CompareVisitor.Result result;
  public TermWithSubtermDoc termDoc1;
  public TermWithSubtermDoc termDoc2;

  public TypeMismatchWithSubexprError(CompareVisitor.Result result, ConcreteSourceNode sourceNode) {
    super("Type mismatch", sourceNode);
    this.result = result;
  }

  @Override
  public Doc getBodyDoc(PrettyPrinterConfig ppConfig) {
    termDoc1 = new TermWithSubtermDoc(result.wholeExpr1, result.subExpr1, result.levels1, ppConfig);
    termDoc2 = new TermWithSubtermDoc(result.wholeExpr2, result.subExpr2, result.levels2, ppConfig);
    Doc expectedDoc = hang(text("Expected type:"), termDoc2);
    return vList(
      expectedDoc,
      hang(text(expectedDoc.getHeight() == 1 ? "  Actual type:" : "Actual type:"), termDoc1));
  }

  @Override
  public boolean hasExpressions() {
    return true;
  }
}
