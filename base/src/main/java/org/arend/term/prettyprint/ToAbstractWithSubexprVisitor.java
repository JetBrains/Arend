package org.arend.term.prettyprint;

import org.arend.core.expr.DefCallExpression;
import org.arend.core.expr.Expression;
import org.arend.core.subst.Levels;
import org.arend.ext.core.expr.CoreExpression;
import org.arend.ext.prettifier.ExpressionPrettifier;
import org.arend.ext.prettyprinting.DefinitionRenamer;
import org.arend.ext.prettyprinting.PrettyPrinterConfig;
import org.arend.naming.renamer.ReferableRenamer;
import org.arend.term.concrete.Concrete;
import org.arend.typechecking.visitor.FindSubexpressionVisitor;

public class ToAbstractWithSubexprVisitor extends ToAbstractVisitor {
  private final Expression mySubexpr;
  private final Levels myLevels;

  public static class Marker {}

  ToAbstractWithSubexprVisitor(ExpressionPrettifier prettifier, PrettyPrinterConfig config, DefinitionRenamer definitionRenamer, CollectFreeVariablesVisitor collector, ReferableRenamer renamer, Expression subexpr, Levels levels) {
    super(prettifier, config, definitionRenamer, collector, renamer);
    mySubexpr = subexpr;
    myLevels = levels;
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

  @Override
  protected boolean convertLevels(DefCallExpression defCall) {
    return mySubexpr == defCall && myLevels != null;
  }
}
