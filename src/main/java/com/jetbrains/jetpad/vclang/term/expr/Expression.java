package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.PrettyPrintable;
import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.error.TypeCheckingError;
import com.jetbrains.jetpad.vclang.term.expr.arg.Argument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;
import com.jetbrains.jetpad.vclang.term.visitor.*;

import java.io.PrintStream;
import java.util.ArrayList;
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

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof Expression)) return false;
    List<CompareVisitor.Equation> result = compare(this, (Expression) obj, CompareVisitor.CMP.EQ);
    return result != null && result.size() == 0;
  }

  @Override
  public void prettyPrint(PrintStream stream, List<String> names, int prec) {
    accept(new PrettyPrintVisitor(stream, names), prec);
  }

  public final Expression liftIndex(int from, int on) {
    return on == 0 ? this : accept(new LiftIndexVisitor(from, on));
  }

  public final Expression subst(Expression substExpr, int from) {
    return accept(new SubstVisitor(substExpr, from));
  }

  public final Expression normalize(NormalizeVisitor.Mode mode) {
    return accept(new NormalizeVisitor(mode));
  }

  public final CheckTypeVisitor.OKResult checkType(Map<String, Definition> globalContext, List<Definition> localContext, Expression expectedType, List<TypeCheckingError> errors) {
    return new CheckTypeVisitor(globalContext, localContext, errors).checkType(this, expectedType);
  }

  public static List<CompareVisitor.Equation> compare(Abstract.Expression expr1, Abstract.Expression expr2, CompareVisitor.CMP cmp) {
    CompareVisitor visitor = new CompareVisitor(cmp, new ArrayList<CompareVisitor.Equation>());
    Boolean result = expr1.accept(visitor, expr2);
    return result ? visitor.equations() : null;
  }

  public static AppExpression App(Expression function, Expression argument, boolean isExplicit) {
    return new AppExpression(function, argument, isExplicit);
  }

  public static Expression Apps(Expression expr, Expression... exprs) {
    for (Expression expr1 : exprs) {
      expr = new AppExpression(expr, expr1, true);
    }
    return expr;
  }

  public static DefCallExpression DefCall(Definition definition) {
    return new DefCallExpression(definition);
  }

  public static IndexExpression Index(int i) {
    return new IndexExpression(i);
  }

  public static LamExpression Lam(List<Argument> arguments, Expression body) {
    return new LamExpression(arguments, body);
  }

  public static PiExpression Pi(List<TypeArgument> arguments, Expression codomain) {
    return new PiExpression(arguments, codomain);
  }

  public static PiExpression Pi(Expression domain, Expression codomain) {
    return new PiExpression(domain, codomain);
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
