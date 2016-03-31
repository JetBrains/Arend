package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.PrettyPrintable;
import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.expr.factory.ConcreteExpressionFactory;
import com.jetbrains.jetpad.vclang.term.expr.visitor.*;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.DummyEquations;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.Equations;
import com.jetbrains.jetpad.vclang.typechecking.normalization.EvalNormalizer;

import java.util.*;

public abstract class Expression implements PrettyPrintable {
  public abstract <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params);

  public abstract Expression getType();

  public ValidateTypeVisitor.ErrorReporter checkType() {
    ValidateTypeVisitor.ErrorReporter reporter = new ValidateTypeVisitor.ErrorReporter();
    accept(new ValidateTypeVisitor(reporter), getType());
    return reporter;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    ToAbstractVisitor visitor = new ToAbstractVisitor(new ConcreteExpressionFactory());
    visitor.addFlags(ToAbstractVisitor.Flag.SHOW_HIDDEN_ARGS).addFlags(ToAbstractVisitor.Flag.SHOW_TYPES_IN_LAM);
    accept(visitor, null).accept(new PrettyPrintVisitor(builder, new ArrayList<String>(), 0), Abstract.Expression.PREC);
    return builder.toString();
  }

  @Override
  public boolean equals(Object obj) {
    return this == obj || obj instanceof Expression && compare(this, (Expression) obj);
  }

  @Override
  public void prettyPrint(StringBuilder builder, List<String> names, byte prec) {
    accept(new ToAbstractVisitor(new ConcreteExpressionFactory()), null).accept(new PrettyPrintVisitor(builder, names, 0), prec);
  }

  public boolean findBinding(Binding binding) {
    return accept(new FindBindingVisitor(Collections.singleton(binding)), null);
  }

  public boolean findBinding(Set<Binding> bindings) {
    return accept(new FindBindingVisitor(bindings), null);
  }

  public final Expression subst(Binding binding, Expression substExpr) {
    return accept(new SubstVisitor(new Substitution(binding, substExpr)), null);
  }

  public final Expression subst(Substitution subst) {
    return subst.getDomain().isEmpty() ? this : accept(new SubstVisitor(subst), null);
  }

  public final Expression normalize(NormalizeVisitor.Mode mode) {
    return accept(new NormalizeVisitor(new EvalNormalizer()), mode);
  }

  public static boolean compare(Expression expr1, Expression expr2, Equations.CMP cmp) {
    return CompareVisitor.compare(DummyEquations.getInstance(), cmp, expr1, expr2, null);
  }

  public static boolean compare(Expression expr1, Expression expr2) {
    return compare(expr1, expr2, Equations.CMP.EQ);
  }

  public Expression getPiParameters(List<DependentLink> params, boolean normalize, boolean implicitOnly) {
    Expression cod = normalize ? normalize(NormalizeVisitor.Mode.WHNF) : this;
    while (cod instanceof PiExpression) {
      if (implicitOnly) {
        if (((PiExpression) cod).getParameters().isExplicit()) {
          break;
        }
        for (DependentLink link = ((PiExpression) cod).getParameters(); link.hasNext(); link = link.getNext()) {
          if (link.isExplicit()) {
            return new PiExpression(link, ((PiExpression) cod).getCodomain());
          }
          if (params != null) {
            params.add(link);
          }
        }
      } else {
        if (params != null) {
          for (DependentLink link = ((PiExpression) cod).getParameters(); link.hasNext(); link = link.getNext()) {
            params.add(link);
          }
        }
      }

      cod = ((PiExpression) cod).getCodomain();
      if (normalize) {
        cod = cod.normalize(NormalizeVisitor.Mode.WHNF);
      }
    }
    return cod;
  }

  public Expression fromPiParameters(List<DependentLink> params) {
    if (!params.isEmpty() && !params.get(0).hasNext()) {
      params = params.subList(1, params.size());
    }
    if (params.isEmpty()) {
      return this;
    }

    Expression result = this;
    for (int i = params.size() - 1; i >= 0; i--) {
      if (i == 0 || !params.get(i - 1).getNext().hasNext()) {
        result = new PiExpression(params.get(i), result);
      }
    }
    return result;
  }

  public Expression getLamParameters(List<DependentLink> params) {
    Expression body = this;
    while (body instanceof LamExpression) {
      if (params != null) {
        for (DependentLink link = ((LamExpression) body).getParameters(); link.hasNext(); link = link.getNext()) {
          params.add(link);
        }
      }
      body = ((LamExpression) body).getBody();
    }
    return body;
  }

  public Expression applyExpressions(List<? extends Expression> expressions) {
    if (expressions.isEmpty()) {
      return this;
    }

    Substitution subst = new Substitution();
    List<DependentLink> params = new ArrayList<>();
    Expression cod = getPiParameters(params, true, false);
    if (params.isEmpty()) {
      assert false;
      return null;
    }
    int size = expressions.size() > params.size() ? params.size() : expressions.size();
    for (int i = 0; i < size; i++) {
      subst.add(params.get(i), expressions.get(i));
    }

    if (expressions.size() < params.size()) {
      cod = cod.fromPiParameters(params.subList(expressions.size(), params.size()));
    }
    return cod.subst(subst).applyExpressions(expressions.subList(size, expressions.size()));
  }

  public Expression getFunction() {
    return this;
  }

  public List<? extends Expression> getArguments() {
    return Collections.emptyList();
  }

  public Expression addArgument(Expression argument, EnumSet<AppExpression.Flag> flag) {
    return new AppExpression(this, new ArrayList<>(Collections.singletonList(argument)), new ArrayList<>(Collections.singletonList(flag)));
  }

  public Expression addArguments(Collection<? extends Expression> arguments, Collection<? extends EnumSet<AppExpression.Flag>> flags) {
    return arguments.isEmpty() ? this : new AppExpression(this, arguments, flags);
  }
}
