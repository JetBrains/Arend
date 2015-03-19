package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.PrettyPrintable;
import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.typechecking.TypeCheckingError;
import com.jetbrains.jetpad.vclang.term.visitor.*;

import java.io.PrintStream;
import java.util.List;
import java.util.Map;

public abstract class Expression implements PrettyPrintable, Abstract.Expression {
  private static final NatExpression NAT = new NatExpression();
  private static final ZeroExpression ZERO = new ZeroExpression();
  private static final SucExpression SUC = new SucExpression();
  private static final NelimExpression NELIM = new NelimExpression();

  public abstract <T> T accept(ExpressionVisitor<? extends T> visitor);

  @Override
  public void setWellTyped(Expression wellTyped) {}

  public final Expression liftIndex(int from, int on) {
    return accept(new LiftIndexVisitor(from, on));
  }

  public final Expression subst(Expression substExpr, int from) {
    return accept(new SubstVisitor(substExpr, from));
  }

  public final Expression normalize(NormalizeVisitor.Mode mode) {
    return accept(new NormalizeVisitor(mode));
  }

  public final void prettyPrint(PrintStream stream, List<String> names, int prec) {
    accept(new PrettyPrintVisitor(stream, names), prec);
  }

  public final CheckTypeVisitor.Result checkType(Map<String, Definition> globalContext, List<Definition> localContext, Expression expectedType, List<TypeCheckingError> errors) {
    return accept(new CheckTypeVisitor(globalContext, localContext, errors), expectedType);
  }

  public static Expression Apps(Expression expr, Expression... exprs) {
    for (Expression expr1 : exprs) {
      expr = new AppExpression(expr, expr1);
    }
    return expr;
  }

  public static DefCallExpression DefCall(Definition definition) {
    return new DefCallExpression(definition);
  }

  public static IndexExpression Index(int i) {
    return new IndexExpression(i);
  }

  public static LamExpression Lam(String variable, Expression body) {
    return new LamExpression(variable, body);
  }

  public static PiExpression Pi(String variable, Expression left, Expression right) {
    return new PiExpression(true, variable, left, right);
  }

  public static PiExpression Pi(boolean explicit, String variable, Expression left, Expression right) {
    return new PiExpression(explicit, variable, left, right);
  }

  public static PiExpression Pi(Expression left, Expression right) {
    return new PiExpression(left, right);
  }

  public static VarExpression Var(String name) {
    return new VarExpression(name);
  }

  public static NatExpression Nat() {
    return NAT;
  }

  public static ZeroExpression Zero() {
    return ZERO;
  }

  public static SucExpression Suc() {
    return SUC;
  }

  public static Expression Suc(Expression expr) {
    return Apps(SUC, expr);
  }

  public static UniverseExpression Universe(int level) {
    return new UniverseExpression(level);
  }

  public static NelimExpression Nelim() {
    return NELIM;
  }

  public static ErrorExpression Error(Expression expr, TypeCheckingError error) {
    return new ErrorExpression(expr, error);
  }
}
