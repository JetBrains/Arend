package org.arend.term.prettyprint;

import org.arend.ext.reference.Precedence;
import org.arend.term.concrete.Concrete;

public class PrettyPrintWithSubexprVisitor extends PrettyPrintVisitor {
  public final static Character MAGIC = '⁀';

  public PrettyPrintWithSubexprVisitor(StringBuilder builder, int indent, boolean doIndent) {
    super(builder, indent, doIndent);
  }

  @Override
  void printExpr(Concrete.Expression expr, Precedence prec) {
    if (expr.getData() instanceof ToAbstractWithSubexprVisitor.Marker) {
      myBuilder.append(MAGIC);
    }
    super.printExpr(expr, prec);
    if (expr.getData() instanceof ToAbstractWithSubexprVisitor.Marker) {
      myBuilder.append(MAGIC);
    }
  }

  @Override
  protected PrettyPrintVisitor copy(StringBuilder builder, int indent, boolean doIndent) {
    return new PrettyPrintWithSubexprVisitor(builder, indent, doIndent);
  }
}
