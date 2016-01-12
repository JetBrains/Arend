package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.PrettyPrintable;
import com.jetbrains.jetpad.vclang.term.expr.factory.ConcreteExpressionFactory;
import com.jetbrains.jetpad.vclang.term.expr.param.Binding;
import com.jetbrains.jetpad.vclang.term.expr.visitor.*;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.DummyEquations;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.Equations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class Expression implements PrettyPrintable {
  public abstract <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params);

  public abstract Expression getType(List<Binding> context);

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
    accept(new ToAbstractVisitor(new ConcreteExpressionFactory(), names), null).accept(new PrettyPrintVisitor(builder, names, 0), prec);
  }

  public String prettyPrint(List<String> names) {
    StringBuilder sb = new StringBuilder();
    prettyPrint(sb, names, Abstract.Expression.PREC);
    return sb.toString();
  }

  public final Expression liftIndex(int from, int on) {
    return on == 0 ? this : accept(new LiftIndexVisitor(on), from);
  }

  public final Expression subst(Binding binding, Expression substExpr) {
    Map<Binding, Expression> substExprs = new HashMap<>();
    substExprs.put(binding, substExpr);
    return accept(new SubstVisitor(substExprs), null);
  }

  public final Expression subst(Map<Binding, Expression> substExprs) {
    return substExprs.isEmpty() ? this : accept(new SubstVisitor(substExprs), null);
  }

  public final Expression normalize(NormalizeVisitor.Mode mode, List<Binding> context) {
    return context == null ? this : accept(new NormalizeVisitor(context), mode);
  }

  public static boolean compare(Expression expr1, Expression expr2, Equations.CMP cmp) {
    return CompareVisitor.compare(DummyEquations.getInstance(), cmp, new ArrayList<Binding>(), expr1, expr2);
  }

  public static boolean compare(Expression expr1, Expression expr2) {
    return compare(expr1, expr2, Equations.CMP.EQ);
  }

  public Expression getFunction(List<Expression> arguments) {
    Expression expr = this;
    while (expr instanceof AppExpression) {
      arguments.add(((AppExpression) expr).getArgument().getExpression());
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
}
