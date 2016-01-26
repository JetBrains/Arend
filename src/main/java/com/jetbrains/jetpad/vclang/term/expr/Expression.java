package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.PrettyPrintable;
import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.expr.factory.ConcreteExpressionFactory;
import com.jetbrains.jetpad.vclang.term.expr.visitor.*;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.DummyEquations;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.Equations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public abstract class Expression implements PrettyPrintable {
  public abstract <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params);

  public abstract Expression getType();

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    prettyPrint(builder, new ArrayList<String>(), (byte) 0);
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

  public String prettyPrint(List<String> names) {
    StringBuilder sb = new StringBuilder();
    prettyPrint(sb, names, Abstract.Expression.PREC);
    return sb.toString();
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
    return accept(new NormalizeVisitor(), mode);
  }

  public static boolean compare(Expression expr1, Expression expr2, Equations.CMP cmp) {
    return CompareVisitor.compare(DummyEquations.getInstance(), cmp, expr1, expr2);
  }

  public static boolean compare(Expression expr1, Expression expr2) {
    return compare(expr1, expr2, Equations.CMP.EQ);
  }

  public Expression getFunction(List<Expression> arguments) {
    Expression expr = this;
    while (expr instanceof AppExpression) {
      if (arguments != null) {
        arguments.add(((AppExpression) expr).getArgument().getExpression());
      }
      expr = ((AppExpression) expr).getFunction();
    }
    return expr;
  }

  public Expression getFunctionArgs(List<ArgumentExpression> arguments) {
    Expression expr = this;
    while (expr instanceof AppExpression) {
      arguments.add(((AppExpression) expr).getArgument());
      expr = ((AppExpression) expr).getFunction();
    }
    return expr;
  }

  public Expression getPiParameters(List<DependentLink> params) {
    Expression cod = this;
    while (cod instanceof PiExpression) {
      if (params != null) {
        for (DependentLink link = ((PiExpression) cod).getParameters(); link != null; link = link.getNext()) {
          params.add(link);
        }
      }
      cod = ((PiExpression) cod).getCodomain();
    }
    return cod;
  }
}
