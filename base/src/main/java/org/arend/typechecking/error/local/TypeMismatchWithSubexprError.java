package org.arend.typechecking.error.local;

import org.arend.core.expr.visitor.CompareVisitor;
import org.arend.ext.concrete.ConcreteSourceNode;
import org.arend.ext.error.TypeMismatchError;

public class TypeMismatchWithSubexprError extends TypeMismatchError {
  public final CompareVisitor.Result result;

  public TypeMismatchWithSubexprError(CompareVisitor.Result result, ConcreteSourceNode sourceNode) {
    super(result.wholeExpr2, result.wholeExpr1, sourceNode);
    this.result = result;
  }
}
