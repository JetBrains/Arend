package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.PrettyPrintable;
import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.typechecking.TypeCheckingException;
import com.jetbrains.jetpad.vclang.term.typechecking.TypeMismatchException;
import com.jetbrains.jetpad.vclang.term.visitor.*;

import java.util.List;

public abstract class Expression implements PrettyPrintable, Abstract.Expression {
  private static final Expression NAT = new NatExpression();
  private static final Expression ZERO = new ZeroExpression();
  private static final Expression SUC = new SucExpression();
  private static final Expression NELIM = new NelimExpression();

  public final Expression liftIndex(int from, int on) {
    return accept(new LiftIndexVisitor(from, on));
  }

  public final Expression subst(Expression substExpr, int from) {
    return accept(new SubstVisitor(substExpr, from));
  }

  public final Expression normalize() {
    return accept(new NormalizeVisitor());
  }

  public final Expression inferType(List<Definition> context) throws TypeCheckingException {
    return accept(new InferTypeVisitor(context));
  }

  public void checkType(List<Definition> context, Expression expected) throws TypeCheckingException {
    Expression actualNorm = inferType(context).normalize();
    Expression expectedNorm = expected.normalize();
    if (!expectedNorm.equals(actualNorm)) {
      throw new TypeMismatchException(expectedNorm, actualNorm, this);
    }
  }

  public static Expression Apps(Expression expr, Expression... exprs) {
    for (Expression expr1 : exprs) {
      expr = new AppExpression(expr, expr1);
    }
    return expr;
  }

  public static Expression DefCall(Definition definition) {
    return new DefCallExpression(definition);
  }

  public static Expression Index(int i) {
    return new IndexExpression(i);
  }

  public static Expression Lam(String variable, Expression body) {
    return new LamExpression(variable, body);
  }

  public static Expression Pi(String variable, Expression left, Expression right) {
    return new PiExpression(true, variable, left, right);
  }

  public static Expression Pi(boolean explicit, String variable, Expression left, Expression right) {
    return new PiExpression(explicit, variable, left, right);
  }

  public static Expression Pi(Expression left, Expression right) {
    return new PiExpression(left, right);
  }

  public static Expression Var(String name) {
    return new VarExpression(name);
  }

  public static Expression Nat() {
    return NAT;
  }

  public static Expression Zero() {
    return ZERO;
  }

  public static Expression Suc() {
    return SUC;
  }

  public static Expression Suc(Expression expr) {
    return Apps(SUC, expr);
  }

  public static Expression Universe(int level) {
    return new UniverseExpression(level);
  }

  public static Expression Nelim() {
    return NELIM;
  }
}
