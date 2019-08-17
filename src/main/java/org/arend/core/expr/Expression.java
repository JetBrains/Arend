package org.arend.core.expr;

import org.arend.core.context.binding.Variable;
import org.arend.core.context.binding.inference.InferenceVariable;
import org.arend.core.context.param.DependentLink;
import org.arend.core.context.param.SingleDependentLink;
import org.arend.core.expr.type.ExpectedType;
import org.arend.core.expr.visitor.*;
import org.arend.core.sort.Sort;
import org.arend.core.subst.ExprSubstitution;
import org.arend.core.subst.LevelSubstitution;
import org.arend.core.subst.SubstVisitor;
import org.arend.error.ErrorReporter;
import org.arend.error.GeneralError;
import org.arend.error.IncorrectExpressionException;
import org.arend.error.doc.Doc;
import org.arend.error.doc.DocFactory;
import org.arend.term.Precedence;
import org.arend.term.concrete.Concrete;
import org.arend.term.prettyprint.PrettyPrintVisitor;
import org.arend.term.prettyprint.PrettyPrinterConfig;
import org.arend.typechecking.implicitargs.equations.DummyEquations;
import org.arend.typechecking.implicitargs.equations.Equations;
import org.arend.util.Decision;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public abstract class Expression implements ExpectedType {
  public abstract <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params);

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    Concrete.Expression expr = ToAbstractVisitor.convert(this, new PrettyPrinterConfig() {
      @Override
      public NormalizeVisitor.Mode getNormalizationMode() {
        return null;
      }
    });
    expr.accept(new PrettyPrintVisitor(builder, 0), new Precedence(Concrete.Expression.PREC));
    return builder.toString();
  }

  @Override
  public boolean equals(Object obj) {
    return this == obj || obj instanceof Expression && compare(this, (Expression) obj, Equations.CMP.EQ);
  }

  public boolean isError() {
    if (!isInstance(ErrorExpression.class)) {
      return false;
    }
    GeneralError error = cast(ErrorExpression.class).getError();
    return error == null || error.level == GeneralError.Level.ERROR;
  }

  @Override
  public void prettyPrint(StringBuilder builder, PrettyPrinterConfig config) {
    ToAbstractVisitor.convert(this, config).accept(new PrettyPrintVisitor(builder, 0, !config.isSingleLine()), new Precedence(Concrete.Expression.PREC));
  }

  @Override
  public Doc prettyPrint(PrettyPrinterConfig ppConfig) {
    return DocFactory.termDoc(this, ppConfig);
  }

  public boolean isLessOrEquals(Expression type, Equations equations, Concrete.SourceNode sourceNode) {
    return CompareVisitor.compare(equations, Equations.CMP.LE, this, type, sourceNode);
  }

  public Sort toSort() {
    UniverseExpression universe = normalize(NormalizeVisitor.Mode.WHNF).checkedCast(UniverseExpression.class);
    return universe == null ? null : universe.getSort();
  }

  public Expression getType(boolean normalizing) {
    if (normalizing) {
      try {
        return accept(GetTypeVisitor.INSTANCE, null);
      } catch (IncorrectExpressionException e) {
        return null;
      }
    } else {
      return accept(GetTypeVisitor.NN_INSTANCE, null);
    }
  }

  public Expression getType() {
    return getType(true);
  }

  public boolean findBinding(Variable binding) {
    return accept(new FindBindingVisitor(Collections.singleton(binding)), null) != null;
  }

  public Variable findBinding(Set<? extends Variable> bindings) {
    return accept(new FindBindingVisitor(bindings), null);
  }

  public Expression strip(ErrorReporter errorReporter) {
    return accept(new StripVisitor(errorReporter), null);
  }

  public Expression copy() {
    return accept(new SubstVisitor(new ExprSubstitution(), LevelSubstitution.EMPTY), null);
  }

  public final Expression subst(Variable binding, Expression substExpr) {
    return accept(new SubstVisitor(new ExprSubstitution(binding, substExpr), LevelSubstitution.EMPTY), null);
  }

  public final Expression subst(ExprSubstitution subst) {
     return subst.isEmpty() ? this : subst(subst, LevelSubstitution.EMPTY);
  }

  public Expression subst(LevelSubstitution subst) {
    return subst.isEmpty() ? this : subst(new ExprSubstitution(), subst);
  }

  public Expression subst(ExprSubstitution exprSubst, LevelSubstitution levelSubst) {
    return exprSubst.isEmpty() && levelSubst.isEmpty() ? this : accept(new SubstVisitor(exprSubst, levelSubst), null);
  }

  public Expression subst(SubstVisitor substVisitor) {
    return accept(substVisitor, null);
  }

  @Override
  public Expression normalize(NormalizeVisitor.Mode mode) {
    return accept(NormalizeVisitor.INSTANCE, mode);
  }

  public static boolean compare(Expression expr1, Expression expr2, Equations.CMP cmp) {
    return CompareVisitor.compare(DummyEquations.getInstance(), cmp, expr1, expr2, null);
  }

  @Override
  public Expression getPiParameters(List<? super SingleDependentLink> params, boolean implicitOnly) {
    Expression cod = normalize(NormalizeVisitor.Mode.WHNF);
    while (cod.isInstance(PiExpression.class)) {
      PiExpression piCod = cod.cast(PiExpression.class);
      if (implicitOnly) {
        if (piCod.getParameters().isExplicit()) {
          break;
        }
        for (SingleDependentLink link = piCod.getParameters(); link.hasNext(); link = link.getNext()) {
          if (link.isExplicit()) {
            return null;
          }
          if (params != null) {
            params.add(link);
          }
        }
      } else {
        if (params != null) {
          for (SingleDependentLink link = piCod.getParameters(); link.hasNext(); link = link.getNext()) {
            params.add(link);
          }
        }
      }

      cod = piCod.getCodomain().normalize(NormalizeVisitor.Mode.WHNF);
    }
    return cod;
  }

  public Expression getLamParameters(List<DependentLink> params) {
    Expression body = this;
    while (body.isInstance(LamExpression.class)) {
      LamExpression lamBody = body.cast(LamExpression.class);
      if (params != null) {
        for (DependentLink link = lamBody.getParameters(); link.hasNext(); link = link.getNext()) {
          params.add(link);
        }
      }
      body = lamBody.getBody();
    }
    return body;
  }

  public Expression applyExpression(Expression expression) {
    return applyExpression(expression, true);
  }

  public Expression applyExpression(Expression expression, boolean normalizing) {
    Expression normExpr = normalizing ? normalize(NormalizeVisitor.Mode.WHNF) : this;
    if (normExpr.isInstance(ErrorExpression.class)) {
      return normExpr;
    }
    PiExpression piExpr = normExpr.checkedCast(PiExpression.class);
    if (piExpr == null) {
      return null;
    }

    SingleDependentLink link = piExpr.getParameters();
    ExprSubstitution subst = new ExprSubstitution(link, expression);
    link = link.getNext();
    Expression result = piExpr.getCodomain();
    if (link.hasNext()) {
      result = new PiExpression(piExpr.getResultSort(), link, result);
    }
    return result.subst(subst);
  }

  public <T extends Expression> T cast(Class<T> clazz) {
    return clazz.cast(this);
  }

  public boolean isInstance(Class clazz) {
    return clazz.isInstance(this);
  }

  public <T extends Expression> T checkedCast(Class<T> clazz) {
    return isInstance(clazz) ? cast(clazz) : null;
  }

  public Expression getFunction() {
    AppExpression app = checkedCast(AppExpression.class);
    return app != null ? app.getFunction() : this;
  }

  public abstract Decision isWHNF();

  // This function assumes that the expression is in a WHNF.
  // If the expression is a constructor, then the function returns null.
  public abstract Expression getStuckExpression();

  public Expression getCanonicalExpression() {
    Expression expr = this;
    while (true) {
      InferenceReferenceExpression refExpr = expr.checkedCast(InferenceReferenceExpression.class);
      if (refExpr == null || refExpr.getSubstExpression() == null) {
        return expr;
      }
      expr = refExpr.getSubstExpression();
    }
  }

  public Expression getCanonicalStuckExpression() {
    Expression stuck = getStuckExpression();
    return stuck == null ? null : stuck.getCanonicalExpression();
  }

  public InferenceVariable getStuckInferenceVariable() {
    Expression stuck = getCanonicalStuckExpression();
    InferenceReferenceExpression infRefExpr = stuck == null ? null : stuck.checkedCast(InferenceReferenceExpression.class);
    return infRefExpr == null ? null : infRefExpr.getVariable();
  }

  public InferenceVariable getInferenceVariable() {
    Expression expr = this;
    while (true) {
      expr = expr.checkedCast(InferenceReferenceExpression.class);
      if (expr == null) {
        return null;
      }
      InferenceVariable var = ((InferenceReferenceExpression) expr).getVariable();
      if (var != null) {
        return var;
      }
      expr = ((InferenceReferenceExpression) expr).getSubstExpression();
    }
  }
}
