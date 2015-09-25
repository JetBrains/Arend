package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.definition.*;
import com.jetbrains.jetpad.vclang.term.expr.arg.Argument;
import com.jetbrains.jetpad.vclang.term.expr.arg.NameArgument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TelescopeArgument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;
import com.jetbrains.jetpad.vclang.term.pattern.ConstructorPattern;
import com.jetbrains.jetpad.vclang.term.pattern.NamePattern;
import com.jetbrains.jetpad.vclang.term.pattern.Pattern;
import com.jetbrains.jetpad.vclang.typechecking.error.TypeCheckingError;

import java.util.*;

public class ExpressionFactory {
  public static Expression Apps(Expression expr, Expression... exprs) {
    for (Expression expr1 : exprs) {
      expr = new AppExpression(expr, new ArgumentExpression(expr1, true, false));
    }
    return expr;
  }

  public static Expression Apps(Expression expr, ArgumentExpression... exprs) {
    for (ArgumentExpression expr1 : exprs) {
      expr = new AppExpression(expr, expr1);
    }
    return expr;
  }

  public static Expression Apps(Expression expr, Expression arg, boolean explicit, boolean hidden) {
    return new AppExpression(expr, new ArgumentExpression(arg, explicit, hidden));
  }

  public static DefCallExpression DefCall(Definition definition) {
    return new DefCallExpression(null, definition, null);
  }

  public static DefCallExpression DefCall(Expression expression, Definition definition) {
    return new DefCallExpression(expression, definition, null);
  }

  public static DefCallExpression DefCall(Expression expression, Definition definition, List<Expression> parameters) {
    return new DefCallExpression(expression, definition, parameters);
  }

  public static ClassExtExpression ClassExt(DefCallExpression baseClassExpression, Map<FunctionDefinition, OverriddenDefinition> definitions, Universe universe) {
    return new ClassExtExpression(baseClassExpression, definitions, universe);
  }

  public static NewExpression New(Expression expression) {
    return new NewExpression(expression);
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

  public static VarExpression Var(String name) {
    return new VarExpression(name);
  }

  public static LetExpression Let(List<LetClause> clauses, Expression expr) {
    return new LetExpression(clauses, expr);
  }

  public static List<LetClause> lets(LetClause... letClauses) {
    return Arrays.asList(letClauses);
  }

  public static LetClause let(String name, Expression term) {
    return new LetClause(name, lamArgs(), null, Abstract.Definition.Arrow.RIGHT, term);
  }

  public static LetClause let(String name, List<Argument> args, Expression term) {
    return new LetClause(name, args, null, Abstract.Definition.Arrow.RIGHT, term);
  }

  public static LetClause let(String name, List<Argument> args, Abstract.Definition.Arrow arrow, Expression term) {
    return new LetClause(name, args, null, arrow, term);
  }

  public static LetClause let(String name, List<Argument> args, Expression resultType, Abstract.Definition.Arrow arrow, Expression term) {
    return new LetClause(name, args, resultType, arrow, term);
  }

  public static List<String> vars(String... vars) {
    return Arrays.asList(vars);
  }

  public static List<TypeArgument> args(TypeArgument... args) {
    return Arrays.asList(args);
  }

  public static List<TelescopeArgument> teleArgs(TelescopeArgument... args) {
    return Arrays.asList(args);
  }

  public static List<NameArgument> nameArgs(NameArgument... args) {
    return Arrays.asList(args);
  }

  public static List<Argument> lamArgs(Argument... args) {
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

  public static SigmaExpression Sigma(List<TypeArgument> arguments) {
    return new SigmaExpression(arguments);
  }

  public static TupleExpression Tuple(List<Expression> fields, SigmaExpression type) {
    return new TupleExpression(fields, type);
  }

  public static TupleExpression Tuple(SigmaExpression type, Expression... fields) {
    return new TupleExpression(Arrays.asList(fields), type);
  }

  public static ProjExpression Proj(Expression expr, int field) {
    return new ProjExpression(expr, field);
  }

  public static DefCallExpression Nat() {
    return DefCall(Prelude.NAT);
  }

  public static DefCallExpression Zero() {
    return DefCall(Prelude.ZERO);
  }

  public static DefCallExpression Suc() {
    return DefCall(Prelude.SUC);
  }

  public static Expression Suc(Expression expr) {
    return Apps(Suc(), expr);
  }

  public static UniverseExpression Universe() {
    return new UniverseExpression(new Universe.Type());
  }

  public static UniverseExpression Universe(int level) {
    return new UniverseExpression(new Universe.Type(level));
  }

  public static UniverseExpression Universe(int level, int truncated) {
    return new UniverseExpression(new Universe.Type(level, truncated));
  }

  public static ErrorExpression Error(Expression expr, TypeCheckingError error) {
    return new ErrorExpression(expr, error);
  }

  public static Expression BinOp(Expression left, Definition binOp, Expression right) {
    return Apps(DefCall(binOp), left, right);
  }

  public static ElimExpression Elim(IndexExpression expression, List<Clause> clauses) {
    return new ElimExpression(expression, clauses);
  }

  public static ElimExpression Elim(List<IndexExpression> expressions, List<Clause> clauses) {
    return new ElimExpression(expressions, clauses);
  }

  public static ConstructorPattern match(boolean isExplicit, Constructor constructor, Pattern... patterns) {
    return new ConstructorPattern(constructor, Arrays.asList(patterns), isExplicit);
  }

  public static ConstructorPattern match(Constructor constructor, Pattern... patterns) {
    return match(true, constructor, patterns);
  }

  public static NamePattern match(boolean isExplicit, String name) {
    return new NamePattern(name, isExplicit);
  }

  public static NamePattern match(String name) {
    return match(true, name);
  }
}
