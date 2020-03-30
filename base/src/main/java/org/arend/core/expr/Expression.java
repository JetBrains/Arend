package org.arend.core.expr;

import org.arend.core.context.binding.Variable;
import org.arend.core.context.binding.inference.BaseInferenceVariable;
import org.arend.core.context.binding.inference.InferenceVariable;
import org.arend.core.context.param.DependentLink;
import org.arend.core.context.param.SingleDependentLink;
import org.arend.core.elimtree.Body;
import org.arend.core.expr.type.Type;
import org.arend.core.expr.type.TypeExpression;
import org.arend.core.expr.visitor.*;
import org.arend.core.sort.Sort;
import org.arend.core.subst.ExprSubstitution;
import org.arend.core.subst.LevelSubstitution;
import org.arend.core.subst.SubstVisitor;
import org.arend.error.IncorrectExpressionException;
import org.arend.ext.core.context.CoreParameter;
import org.arend.ext.core.expr.CoreExpression;
import org.arend.ext.core.ops.CMP;
import org.arend.ext.core.ops.ExpressionMapper;
import org.arend.ext.core.ops.NormalizationMode;
import org.arend.ext.prettyprinting.PrettyPrinterConfig;
import org.arend.ext.prettyprinting.doc.Doc;
import org.arend.ext.prettyprinting.doc.DocFactory;
import org.arend.ext.reference.Precedence;
import org.arend.prelude.Prelude;
import org.arend.term.concrete.Concrete;
import org.arend.term.prettyprint.PrettyPrintVisitor;
import org.arend.typechecking.implicitargs.equations.DummyEquations;
import org.arend.typechecking.implicitargs.equations.Equations;
import org.arend.util.Decision;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public abstract class Expression implements Body, CoreExpression {
  public abstract <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params);

  public abstract <P1, P2, R> R accept(ExpressionVisitor2<? super P1, ? super P2, ? extends R> visitor, P1 param1, P2 param2);

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    Concrete.Expression expr = ToAbstractVisitor.convert(this, new PrettyPrinterConfig() {
      @Override
      public NormalizationMode getNormalizationMode() {
        return null;
      }
    });
    expr.accept(new PrettyPrintVisitor(builder, 0), new Precedence(Concrete.Expression.PREC));
    return builder.toString();
  }

  @TestOnly
  @Override
  public boolean equals(Object obj) {
    return this == obj || obj instanceof Expression && compare(this, (Expression) obj, null, CMP.EQ);
  }

  @Override
  public boolean isError() {
    ErrorExpression errorExpr = cast(ErrorExpression.class);
    return errorExpr != null && errorExpr.isError();
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
    return CompareVisitor.compare(equations, CMP.LE, this, type, Type.OMEGA, sourceNode);
  }

  public Sort toSort() {
    UniverseExpression universe = normalize(NormalizationMode.WHNF).cast(UniverseExpression.class);
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

  public Expression copy() {
    return accept(new SubstVisitor(new ExprSubstitution(), LevelSubstitution.EMPTY), null);
  }

  public final Expression subst(Variable binding, Expression substExpr) {
    if (substExpr instanceof ReferenceExpression && ((ReferenceExpression) substExpr).getBinding() == binding) {
      return this;
    }
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

  public Type subst(SubstVisitor substVisitor) {
    Expression result = substVisitor.isEmpty() ? this : accept(substVisitor, null);
    if (result instanceof Type) {
      return (Type) result;
    }

    Sort sort = result.getSortOfType();
    if (sort == null) {
      throw new SubstVisitor.SubstException();
    }
    return new TypeExpression(result, sort);
  }

  @NotNull
  @Override
  public Expression normalize(@NotNull NormalizationMode mode) {
    return accept(NormalizeVisitor.INSTANCE, mode);
  }

  @Nullable
  @Override
  public CoreExpression replaceSubexpressions(@NotNull ExpressionMapper mapper) {
    try {
      return accept(new RecreateExpressionVisitor(mapper), null);
    } catch (SubstVisitor.SubstException e) {
      return null;
    }
  }

  public static boolean compare(Expression expr1, Expression expr2, Expression type, CMP cmp) {
    return CompareVisitor.compare(DummyEquations.getInstance(), cmp, expr1, expr2, type, null);
  }

  @Override
  public boolean compare(@NotNull CoreExpression expr2, @NotNull CMP cmp) {
    return expr2 instanceof Expression && CompareVisitor.compare(DummyEquations.getInstance(), cmp, this, (Expression) expr2, null, null);
  }

  @Nullable
  @Override
  public Expression removeConstLam() {
    return ElimBindingVisitor.elimLamBinding(normalize(NormalizationMode.WHNF).cast(LamExpression.class));
  }

  @Nullable
  @Override
  public FunCallExpression toEquality() {
    Expression expr = getUnderlyingExpression();
    if (expr instanceof FunCallExpression && ((FunCallExpression) expr).getDefinition() == Prelude.PATH_INFIX) {
      return (FunCallExpression) expr;
    }
    DataCallExpression dataCall = expr.normalize(NormalizationMode.WHNF).cast(DataCallExpression.class);
    if (dataCall != null && dataCall.getDefinition() == Prelude.PATH) {
      List<Expression> args = dataCall.getDefCallArguments();
      Expression type = args.get(0).removeConstLam();
      if (type != null) {
        return new FunCallExpression(Prelude.PATH_INFIX, dataCall.getSortArgument(), Arrays.asList(type, args.get(1), args.get(2)));
      }
    }

    return null;
  }

  public Expression dropPiParameter(int n) {
    if (n == 0) {
      return this;
    }

    Expression cod = normalize(NormalizationMode.WHNF);
    for (PiExpression piCod = cod.cast(PiExpression.class); piCod != null; piCod = cod.cast(PiExpression.class)) {
      SingleDependentLink link = piCod.getParameters();
      while (n > 0 && link.hasNext()) {
        link = link.getNext();
        n--;
      }
      if (n == 0) {
        return link.hasNext() ? new PiExpression(piCod.getResultSort(), link, piCod.getCodomain()) : piCod.getCodomain();
      }
      cod = piCod.getCodomain().normalize(NormalizationMode.WHNF);
    }

    return null;
  }

  @Override
  public @NotNull CoreExpression getPiParameters(@Nullable List<? super CoreParameter> parameters) {
    return getPiParameters(parameters, false);
  }

  public Expression getPiParameters(List<? super SingleDependentLink> params, boolean implicitOnly) {
    Expression cod = normalize(NormalizationMode.WHNF);
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

      cod = piCod.getCodomain().normalize(NormalizationMode.WHNF);
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
    Expression normExpr = (normalizing ? normalize(NormalizationMode.WHNF) : this).getUnderlyingExpression();
    return normExpr instanceof ErrorExpression ? normExpr : normExpr instanceof PiExpression ? normExpr.applyExpression(expression, normalizing) : null;
  }

  public boolean canBeConstructor() {
    return true;
  }

  @NotNull
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

  public InferenceVariable getStuckInferenceVariable() {
    Expression stuck = getStuckExpression();
    return stuck instanceof InferenceReferenceExpression && ((InferenceReferenceExpression) stuck).getVariable() instanceof InferenceVariable ? (InferenceVariable) ((InferenceReferenceExpression) stuck).getVariable() : null;
  }

  public InferenceVariable getInferenceVariable() {
    Expression expr = this;
    while (true) {
      expr = expr.cast(InferenceReferenceExpression.class);
      if (expr == null) {
        return null;
      }
      BaseInferenceVariable var = ((InferenceReferenceExpression) expr).getVariable();
      if (var instanceof InferenceVariable) {
        return (InferenceVariable) var;
      }
      expr = ((InferenceReferenceExpression) expr).getSubstExpression();
      if (expr == null) {
        return null;
      }
    }
  }
}
