package com.jetbrains.jetpad.vclang.term.visitor;

import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.expr.arg.Argument;
import com.jetbrains.jetpad.vclang.term.expr.arg.NameArgument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TelescopeArgument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;

import java.util.ArrayList;
import java.util.List;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;

public class SubstVisitor implements ExpressionVisitor<Expression> {
  private final Expression mySubstExpr;
  private final int myFrom;

  public SubstVisitor(Expression substExpr, int from) {
    mySubstExpr = substExpr;
    myFrom = from;
  }

  @Override
  public Expression visitApp(AppExpression expr) {
    return Apps(expr.getFunction().accept(this), expr.getArgument().accept(this));
  }

  @Override
  public Expression visitDefCall(DefCallExpression expr) {
    return expr;
  }

  @Override
  public Expression visitIndex(IndexExpression expr) {
    if (expr.getIndex() < myFrom) return Index(expr.getIndex());
    if (expr.getIndex() == myFrom) return mySubstExpr;
    return Index(expr.getIndex() - 1);
  }

  @Override
  public Expression visitLam(LamExpression expr) {
    List<Argument> arguments = new ArrayList<>();
    Expression substExpr = mySubstExpr;
    int from = myFrom;
    int on = 0;
    for (Argument argument : expr.getArguments()) {
      if (argument instanceof NameArgument) {
        arguments.add(argument);
        ++on;
      } else
      if (argument instanceof TelescopeArgument) {
        from += on;
        TelescopeArgument teleArgument = (TelescopeArgument) argument;
        substExpr = substExpr.liftIndex(0, on);
        arguments.add(new TelescopeArgument(argument.getExplicit(), teleArgument.getNames(), teleArgument.getType().subst(substExpr, from)));
        on = teleArgument.getNames().size();
      } else {
        throw new IllegalStateException();
      }
    }
    return Lam(arguments, expr.getBody().subst(substExpr.liftIndex(0, on), from + on));
  }

  @Override
  public Expression visitNat(NatExpression expr) {
    return expr;
  }

  @Override
  public Expression visitNelim(NelimExpression expr) {
    return expr;
  }

  private Expression visitArguments(List<TypeArgument> arguments, Expression codomain) {
    Expression substExpr = mySubstExpr;
    int from = myFrom;
    List<TypeArgument> result = new ArrayList<>();
    for (TypeArgument argument : arguments) {
      if (argument instanceof TelescopeArgument) {
        List<String> names = ((TelescopeArgument) argument).getNames();
        result.add(new TelescopeArgument(argument.getExplicit(), names, argument.getType().subst(substExpr, from)));
        substExpr = substExpr.liftIndex(0, names.size());
        from += names.size();
      } else {
        result.add(new TypeArgument(argument.getExplicit(), argument.getType().subst(substExpr, from)));
        substExpr = substExpr.liftIndex(0, 1);
        ++from;
      }
    }
    return codomain == null ? Sigma(result) : Pi(result, codomain.subst(substExpr, from));
  }

  @Override
  public Expression visitPi(PiExpression expr) {
    return visitArguments(expr.getArguments(), expr.getCodomain());
  }

  @Override
  public Expression visitSuc(SucExpression expr) {
    return expr;
  }

  @Override
  public Expression visitUniverse(UniverseExpression expr) {
    return expr;
  }

  @Override
  public Expression visitVar(VarExpression expr) {
    return expr;
  }

  @Override
  public Expression visitZero(ZeroExpression expr) {
    return expr;
  }

  @Override
  public Expression visitHole(HoleExpression expr) {
    return expr.getInstance(expr.expression() == null ? null : expr.expression().accept(this));
  }

  @Override
  public Expression visitTuple(TupleExpression expr) {
    List<Expression> fields = new ArrayList<>(expr.getFields().size());
    for (Expression field : expr.getFields()) {
      fields.add(field.accept(this));
    }
    return Tuple(fields);
  }

  @Override
  public Expression visitSigma(SigmaExpression expr) {
    return visitArguments(expr.getArguments(), null);
  }
}
