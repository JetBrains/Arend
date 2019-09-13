package org.arend.core.definition;

import org.arend.core.context.param.DependentLink;
import org.arend.core.expr.Expression;
import org.arend.core.expr.visitor.CompareVisitor;
import org.arend.core.subst.ExprSubstitution;
import org.arend.typechecking.implicitargs.equations.DummyEquations;
import org.arend.typechecking.implicitargs.equations.Equations;

import java.util.List;

public class ParametersLevel {
  public final DependentLink parameters;
  public int level;

  public ParametersLevel(DependentLink parameters, int level) {
    this.parameters = parameters;
    this.level = level;
  }

  public boolean checkExpressionsTypes(List<? extends Expression> exprList) {
    if (parameters == null) {
      return true;
    }

    DependentLink link = parameters;
    ExprSubstitution substitution = new ExprSubstitution();
    for (Expression expr : exprList) {
      Expression type = expr.getType();
      if (type == null || !CompareVisitor.compare(DummyEquations.getInstance(), Equations.CMP.LE, type, link.getTypeExpr().subst(substitution), null)) {
        return false;
      }
      substitution.add(link, expr);
      link = link.getNext();
    }
    return true;
  }

  public boolean hasEquivalentDomain(ParametersLevel another) {
    if (parameters == null && another.parameters == null) {
      return true;
    }

    if (parameters == null || another.parameters == null) {
      return false;
    }

    return new CompareVisitor(DummyEquations.getInstance(), Equations.CMP.EQ, null).compareParameters(parameters, another.parameters);
  }

  public void mergeCodomain(ParametersLevel another) {
    level = Math.min(level, another.level);
  }
}
