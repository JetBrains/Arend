package org.arend.core.expr;

import org.arend.core.context.binding.Variable;
import org.arend.core.context.binding.inference.InferenceVariable;
import org.arend.core.context.param.DependentLink;
import org.arend.core.context.param.SingleDependentLink;
import org.arend.core.elimtree.Body;
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
import org.arend.ext.core.expr.CoreExpression;
import org.arend.ext.core.ops.ExpressionMapper;
import org.arend.ext.reference.Precedence;
import org.arend.term.concrete.Concrete;
import org.arend.term.prettyprint.PrettyPrintVisitor;
import org.arend.term.prettyprint.PrettyPrinterConfig;
import org.arend.typechecking.implicitargs.equations.DummyEquations;
import org.arend.typechecking.implicitargs.equations.Equations;
import org.arend.util.Decision;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public abstract class Expression implements ExpectedType, Body, CoreExpression {
  public abstract <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params);

  public abstract <P1, P2, R> R accept(ExpressionVisitor2<? super P1, ? super P2, ? extends R> visitor, P1 param1, P2 param2);

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

  // Only for tests
  @Override
  public boolean equals(Object obj) {
    return this == obj || obj instanceof Expression && compare(this, (Expression) obj, null, Equations.CMP.EQ);
  }

  public boolean isError() {
    ErrorExpression errorExpr = cast(ErrorExpression.class);
    return errorExpr != null && (errorExpr.getError() == null || errorExpr.getError().level == GeneralError.Level.ERROR);
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
    return CompareVisitor.compare(equations, Equations.CMP.LE, this, type, ExpectedType.OMEGA, sourceNode);
  }

  public Sort toSort() {
    UniverseExpression universe = normalize(NormalizeVisitor.Mode.WHNF).cast(UniverseExpression.class);
    return universe == null ? null : universe.getSort();
  }

  public Sort getSortOfType() {
    Expression type = getType();
    return type == null ? null : type.toSort();
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

  @Override
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

  @Nullable
  @Override
  public CoreExpression recreate(@Nonnull ExpressionMapper mapper) {
    try {
      // TODO[lang_ext]: Check that the result is correct
      return accept(new RecreateExpressionVisitor(mapper), null);
    } catch (SubstVisitor.SubstException e) {
      return null;
    }
  }

  public static boolean compare(Expression expr1, Expression expr2, ExpectedType type, Equations.CMP cmp) {
    return CompareVisitor.compare(DummyEquations.getInstance(), cmp, expr1, expr2, type, null);
  }

  public Expression dropPiParameter(int n) {
    if (n == 0) {
      return this;
    }

    Expression cod = normalize(NormalizeVisitor.Mode.WHNF);
    for (PiExpression piCod = cod.cast(PiExpression.class); piCod != null; piCod = cod.cast(PiExpression.class)) {
      SingleDependentLink link = piCod.getParameters();
      while (n > 0 && link.hasNext()) {
        link = link.getNext();
        n--;
      }
      if (n == 0) {
        return link.hasNext() ? new PiExpression(piCod.getResultSort(), link, piCod.getCodomain()) : piCod.getCodomain();
      }
      cod = piCod.getCodomain().normalize(NormalizeVisitor.Mode.WHNF);
    }

    return null;
  }

  @Override
  public Expression getPiParameters(List<? super SingleDependentLink> params, boolean implicitOnly) {
    Expression cod = normalize(NormalizeVisitor.Mode.WHNF);
    for (PiExpression piCod = cod.cast(PiExpression.class); piCod != null; piCod = cod.cast(PiExpression.class)) {
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
    for (LamExpression lamBody = body.cast(LamExpression.class); lamBody != null; lamBody = body.cast(LamExpression.class)) {
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
    Expression normExpr = (normalizing ? normalize(NormalizeVisitor.Mode.WHNF) : this).getUnderlyingExpression();
    return normExpr instanceof ErrorExpression ? normExpr : normExpr instanceof PiExpression ? normExpr.applyExpression(expression, normalizing) : null;
  }

  public boolean canBeConstructor() {
    return true;
  }

  @Nonnull
  @Override
  public Expression getUnderlyingExpression() {
    return this;
  }

  public boolean isInstance(Class clazz) {
    return clazz.isInstance(this);
  }

  public <T extends Expression> T cast(Class<T> clazz) {
    return clazz.isInstance(this) ? clazz.cast(this) : null;
  }

  public Expression getFunction() {
    AppExpression app = cast(AppExpression.class);
    return app != null ? app.getFunction() : this;
  }

  public Expression getArguments(List<Expression> args) {
    return this;
  }

  @Override
  public Decision isWHNF(List<? extends Expression> arguments) {
    return Decision.NO;
  }

  @Override
  public Expression getStuckExpression(List<? extends Expression> arguments, Expression expression) {
    return null;
  }

  public abstract Decision isWHNF();

  // This function assumes that the expression is in a WHNF.
  // If the expression is a constructor, then the function returns null.
  public abstract Expression getStuckExpression();

  public Expression getCanonicalExpression() {
    Expression expr = this;
    while (true) {
      InferenceReferenceExpression refExpr = expr.cast(InferenceReferenceExpression.class);
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
    InferenceReferenceExpression infRefExpr = stuck == null ? null : stuck.cast(InferenceReferenceExpression.class);
    return infRefExpr == null ? null : infRefExpr.getVariable();
  }

  public InferenceVariable getInferenceVariable() {
    Expression expr = this;
    while (true) {
      expr = expr.cast(InferenceReferenceExpression.class);
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
