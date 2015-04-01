package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.error.TypeCheckingError;
import com.jetbrains.jetpad.vclang.term.expr.arg.Argument;
import com.jetbrains.jetpad.vclang.term.expr.arg.NameArgument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TelescopeArgument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ExpressionFactory {
  private static final NatExpression NAT = new NatExpression();
  private static final ZeroExpression ZERO = new ZeroExpression();
  private static final SucExpression SUC = new SucExpression();
  private static final NelimExpression NELIM = new NelimExpression();

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

  public static LamExpression Lam(String var, Expression body) {
    List<Argument> arguments = new ArrayList<>(1);
    arguments.add(new NameArgument(true, var));
    return Lam(arguments, body);
  }

  public static List<String> vars(String... vars) {
    return Arrays.asList(vars);
  }

  public static List<TypeArgument> args(TypeArgument... args) {
    return Arrays.asList(args);
  }

  public static List<Argument> argsLam(Argument... args) {
    return Arrays.asList(args);
  }

  public static NameArgument Name(boolean explicit, String name) {
    return new NameArgument(explicit, name);
  }

  public static NameArgument Name(String name) {
    return new NameArgument(true, name);
  }

  public static TypeArgument TypeArg(boolean explicit, Expression type) {
    return new TypeArgument(explicit, type);
  }

  public static TypeArgument TypeArg(Expression type) {
    return new TypeArgument(true, type);
  }

  public static TelescopeArgument Tele(boolean explicit, List<String> names, Expression type) {
    return new TelescopeArgument(explicit, names, type);
  }

  public static TelescopeArgument Tele(List<String> names, Expression type) {
    return new TelescopeArgument(true, names, type);
  }

  public static PiExpression Pi(List<TypeArgument> arguments, Expression codomain) {
    return new PiExpression(arguments, codomain);
  }

  public static PiExpression Pi(boolean explicit, String var, Expression domain, Expression codomain) {
    List<TypeArgument> arguments = new ArrayList<>(1);
    List<String> vars = new ArrayList<>(1);
    vars.add(var);
    arguments.add(new TelescopeArgument(explicit, vars, domain));
    return new PiExpression(arguments, codomain);
  }

  public static PiExpression Pi(String var, Expression domain, Expression codomain) {
    return Pi(true, var, domain, codomain);
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
