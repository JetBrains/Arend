package com.jetbrains.jetpad.vclang.core.expr;

import com.jetbrains.jetpad.vclang.core.context.binding.Binding;
import com.jetbrains.jetpad.vclang.core.context.binding.Variable;
import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.context.param.SingleDependentLink;
import com.jetbrains.jetpad.vclang.core.expr.factory.ConcreteExpressionFactory;
import com.jetbrains.jetpad.vclang.core.expr.type.ExpectedType;
import com.jetbrains.jetpad.vclang.core.expr.visitor.*;
import com.jetbrains.jetpad.vclang.core.sort.Sort;
import com.jetbrains.jetpad.vclang.core.subst.ExprSubstitution;
import com.jetbrains.jetpad.vclang.core.subst.LevelSubstitution;
import com.jetbrains.jetpad.vclang.core.subst.SubstVisitor;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.prettyprint.PrettyPrintVisitor;
import com.jetbrains.jetpad.vclang.typechecking.error.LocalErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.DummyEquations;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.Equations;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public abstract class Expression implements ExpectedType {
  public abstract <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params);

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    ToAbstractVisitor visitor = new ToAbstractVisitor(new ConcreteExpressionFactory());
    visitor
      .addFlags(ToAbstractVisitor.Flag.SHOW_IMPLICIT_ARGS)
      .addFlags(ToAbstractVisitor.Flag.SHOW_TYPES_IN_LAM)
      .addFlags(ToAbstractVisitor.Flag.SHOW_CON_PARAMS);
    accept(visitor, null).accept(new PrettyPrintVisitor(builder, 0), Abstract.Expression.PREC);
    return builder.toString();
  }

  @Override
  public boolean equals(Object obj) {
    return this == obj || obj instanceof Expression && compare(this, (Expression) obj, Equations.CMP.EQ);
  }

  @Override
  public void prettyPrint(StringBuilder builder, List<String> names, byte prec, int indent) {
    ToAbstractVisitor visitor = new ToAbstractVisitor(new ConcreteExpressionFactory(), names);
    visitor.addFlags(ToAbstractVisitor.Flag.SHOW_IMPLICIT_ARGS).addFlags(ToAbstractVisitor.Flag.SHOW_TYPES_IN_LAM);
    accept(visitor, null).accept(new PrettyPrintVisitor(builder, indent), prec);
  }

  public boolean isLessOrEquals(Expression type, Equations equations, Abstract.SourceNode sourceNode) {
    return CompareVisitor.compare(equations, Equations.CMP.LE, this, type, sourceNode);
  }

  public Sort toSort() {
    UniverseExpression universe = normalize(NormalizeVisitor.Mode.WHNF).checkedCast(UniverseExpression.class);
    return universe == null ? null : universe.getSort();
  }

  public Expression getType() {
    return accept(new GetTypeVisitor(), null);
  }

  public boolean findBinding(Variable binding) {
    return accept(new FindBindingVisitor(Collections.singleton(binding)), null) != null;
  }

  public Variable findBinding(Set<? extends Variable> bindings) {
    return this.accept(new FindBindingVisitor(bindings), null);
  }

  public Expression strip(Set<Binding> bounds, LocalErrorReporter errorReporter) {
    return accept(new StripVisitor(bounds, errorReporter), null);
  }

  public Expression copy() {
    return accept(new SubstVisitor(new ExprSubstitution(), LevelSubstitution.EMPTY), null);
  }

  public final Expression subst(Binding binding, Expression substExpr) {
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

  @Override
  public Expression normalize(NormalizeVisitor.Mode mode) {
    return accept(new NormalizeVisitor(), mode);
  }

  public static boolean compare(Expression expr1, Expression expr2, Equations.CMP cmp) {
    return CompareVisitor.compare(DummyEquations.getInstance(), cmp, expr1, expr2, null);
  }

  @Override
  public Expression getPiParameters(List<SingleDependentLink> params, boolean implicitOnly) {
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
    Expression normExpr = normalize(NormalizeVisitor.Mode.WHNF);
    if (normExpr.isInstance(ErrorExpression.class)) {
      return normExpr;
    }
    PiExpression piExpr = normExpr.cast(PiExpression.class);
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

  public <T extends Expression> boolean isInstance(Class<T> clazz) {
    return clazz.isInstance(this);
  }

  public <T extends Expression> T checkedCast(Class<T> clazz) {
    return isInstance(clazz) ? cast(clazz) : null;
  }

  public abstract boolean isWHNF();

  // This function assumes that the expression is in a WHNF.
  // If the expression is a constructor, then the function returns null.
  public abstract Expression getStuckExpression();
}
