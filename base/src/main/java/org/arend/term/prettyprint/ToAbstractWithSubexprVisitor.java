package org.arend.term.prettyprint;

import org.arend.core.expr.Expression;
import org.arend.ext.core.expr.CoreExpression;
import org.arend.ext.prettyprinting.DefinitionRenamer;
import org.arend.ext.prettyprinting.PrettyPrinterConfig;
import org.arend.naming.renamer.ReferableRenamer;
import org.arend.term.concrete.Concrete;
import org.arend.typechecking.visitor.FindSubexpressionVisitor;

public class ToAbstractWithSubexprVisitor extends ToAbstractVisitor {
  private final Expression mySubexpr;

  public static class Marker {}

  ToAbstractWithSubexprVisitor(PrettyPrinterConfig config, DefinitionRenamer definitionRenamer, CollectFreeVariablesVisitor collector, ReferableRenamer renamer, Expression subexpr) {
    super(config, definitionRenamer, collector, renamer);
    mySubexpr = subexpr;
  }

  @Override
  Concrete.Expression convertExpr(Expression expr) {
    Concrete.Expression result = super.convertExpr(expr);
    if (mySubexpr == expr) {
      result.setData(new Marker());
    }
    return result;
  }

  @Override
  protected boolean convertSubexpr(Expression expr) {
    return expr.accept(new FindSubexpressionVisitor(sub -> sub == mySubexpr ? CoreExpression.FindAction.STOP : CoreExpression.FindAction.CONTINUE), null);
  }
}
