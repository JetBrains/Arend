package com.jetbrains.jetpad.vclang.term;

import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.definition.Universe;

import java.util.Arrays;
import java.util.List;

public class ConcreteExpressionFactory {
  public static final Concrete.Position POSITION = new Concrete.Position(0, 0);

  public static Concrete.LamExpression cLam(List<Concrete.Argument> arguments, Concrete.Expression body) {
    return new Concrete.LamExpression(POSITION, arguments, body);
  }

  public static Concrete.LamExpression cLam(String var, Concrete.Expression body) {
    return cLam(cargs(cName(var)), body);
  }

  public static Concrete.DefCallExpression cVar(String name) {
    return new Concrete.DefCallExpression(POSITION, null, name == null ? "_" : name);
  }

  public static Concrete.DefCallExpression cDefCall(Concrete.Expression expr, Definition definition) {
    Concrete.DefCallExpression result = new Concrete.DefCallExpression(POSITION, expr, definition.getName());
    result.setResolvedDefinition(definition);
    return result;
  }

  public static Concrete.ClassExtExpression cClassExt(Concrete.Expression expr, List<Concrete.ImplementStatement> definitions) {
    return new Concrete.ClassExtExpression(POSITION, expr, definitions);
  }

  public static Concrete.ImplementStatement cImplStatement(String name, Concrete.Expression expr) {
    return new Concrete.ImplementStatement(POSITION, name, expr);
  }

  public static Concrete.Expression cApps(Concrete.Expression expr, Concrete.Expression... exprs) {
    for (Concrete.Expression expr1 : exprs) {
      expr = new Concrete.AppExpression(POSITION, expr, new Concrete.ArgumentExpression(expr1, true, false));
    }
    return expr;
  }

  public static Concrete.Expression cApps(Concrete.Expression expr, Concrete.Expression arg, boolean explicit, boolean hidden) {
    return new Concrete.AppExpression(POSITION, expr, new Concrete.ArgumentExpression(arg, explicit, hidden));
  }

  public static Concrete.DefCallExpression cNat() {
    return new Concrete.DefCallExpression(POSITION, Prelude.NAT);
  }

  public static Concrete.DefCallExpression cZero() {
    return new Concrete.DefCallExpression(POSITION, Prelude.ZERO);
  }

  public static Concrete.DefCallExpression cSuc() {
    return new Concrete.DefCallExpression(POSITION, Prelude.SUC);
  }

  public static Concrete.Expression cSuc(Concrete.Expression expr) {
    return cApps(cSuc(), expr);
  }

  public static Concrete.LetExpression cLet(List<Concrete.LetClause> clauses, Concrete.Expression expr) {
    return new Concrete.LetExpression(POSITION, clauses, expr);
  }

  public static List<Concrete.LetClause> clets(Concrete.LetClause... letClauses) {
    return Arrays.asList(letClauses);
  }

  public static Concrete.LetClause clet(String name, Concrete.Expression term) {
    return new Concrete.LetClause(POSITION, name, cargs(), null, Abstract.Definition.Arrow.RIGHT, term);
  }

  public static Concrete.LetClause clet(String name, List<Concrete.Argument> args, Concrete.Expression term) {
    return new Concrete.LetClause(POSITION, name, args, null, Abstract.Definition.Arrow.RIGHT, term);
  }

  public static Concrete.LetClause clet(String name, List<Concrete.Argument> args, Abstract.Definition.Arrow arrow, Concrete.Expression term) {
    return new Concrete.LetClause(POSITION, name, args, null, arrow, term);
  }

  public static Concrete.LetClause clet(String name, List<Concrete.Argument> args, Concrete.Expression resultType, Abstract.Definition.Arrow arrow, Concrete.Expression term) {
    return new Concrete.LetClause(POSITION, name, args, resultType, arrow, term);
  }

  public static List<String> cvars(String... vars) {
    return Arrays.asList(vars);
  }

  public static List<Concrete.Argument> cargs(Concrete.Argument... args) {
    return Arrays.asList(args);
  }

  public static List<Concrete.TypeArgument> ctypeArgs(Concrete.TypeArgument... args) {
    return Arrays.asList(args);
  }

  public static Concrete.NameArgument cName(String name) {
    return new Concrete.NameArgument(POSITION, true, name);
  }

  public static Concrete.NameArgument cName(boolean explicit, String name) {
    return new Concrete.NameArgument(POSITION, explicit, name);
  }

  public static Concrete.TypeArgument cTypeArg(boolean explicit, Concrete.Expression type) {
    return new Concrete.TypeArgument(explicit, type);
  }

  public static Concrete.TypeArgument cTypeArg(Concrete.Expression type) {
    return new Concrete.TypeArgument(true, type);
  }

  public static Concrete.TelescopeArgument cTele(List<String> names, Concrete.Expression type) {
    return new Concrete.TelescopeArgument(POSITION, true, names, type);
  }

  public static Concrete.TelescopeArgument cTele(boolean explicit, List<String> names, Concrete.Expression type) {
    return new Concrete.TelescopeArgument(POSITION, explicit, names, type);
  }

  public static Concrete.PiExpression cPi(Concrete.Expression domain, Concrete.Expression codomain) {
    return new Concrete.PiExpression(POSITION, ctypeArgs(cTypeArg(domain)), codomain);
  }

  public static Concrete.Expression cPi(List<Concrete.TypeArgument> arguments, Concrete.Expression codomain) {
    return arguments.isEmpty() ? codomain : new Concrete.PiExpression(POSITION, arguments, codomain);
  }

  public static Concrete.PiExpression cPi(boolean explicit, String var, Concrete.Expression domain, Concrete.Expression codomain) {
    return (Concrete.PiExpression) cPi(ctypeArgs(cTele(explicit, cvars(var), domain)), codomain);
  }

  public static Concrete.PiExpression cPi(String var, Concrete.Expression domain, Concrete.Expression codomain) {
    return cPi(true, var, domain, codomain);
  }

  public static Concrete.ErrorExpression cError() {
    return new Concrete.ErrorExpression(POSITION);
  }

  public static Concrete.InferHoleExpression cInferHole() {
    return new Concrete.InferHoleExpression(POSITION);
  }

  public static Concrete.TupleExpression cTuple(List<Concrete.Expression> fields) {
    return new Concrete.TupleExpression(POSITION, fields);
  }

  public static Concrete.SigmaExpression cSigma(List<Concrete.TypeArgument> args) {
    return new Concrete.SigmaExpression(POSITION, args);
  }

  public static Concrete.ProjExpression cProj(Concrete.Expression expr, int field) {
    return new Concrete.ProjExpression(POSITION, expr, field);
  }

  public static Concrete.NewExpression cNew(Concrete.Expression expr) {
    return new Concrete.NewExpression(POSITION, expr);
  }

  public static Concrete.ElimExpression cElim(List<Concrete.Expression> expressions, List<Concrete.Clause> clauses) {
    return new Concrete.ElimExpression(POSITION, expressions, clauses);
  }

  public static Concrete.ElimExpression cElim(List<Concrete.Expression> expressions, Concrete.Clause... clauses) {
    return cElim(expressions, Arrays.asList(clauses));
  }

  public static Concrete.CaseExpression cCase(List<Concrete.Expression> expressions, List<Concrete.Clause> clauses) {
    return new Concrete.CaseExpression(POSITION, expressions, clauses);
  }

  public static Concrete.Clause cClause(List<Concrete.Pattern> patterns, Abstract.Definition.Arrow arrow, Concrete.Expression expr) {
    return new Concrete.Clause(POSITION, patterns, arrow, expr);
  }

  public static Concrete.UniverseExpression cUniverse(Universe universe) {
    return new Concrete.UniverseExpression(POSITION, universe);
  }

  public static Concrete.UniverseExpression cUniverse() {
    return new Concrete.UniverseExpression(POSITION, new Universe.Type());
  }

  public static Concrete.UniverseExpression cUniverse(int level) {
    return new Concrete.UniverseExpression(POSITION, new Universe.Type(level));
  }

  public static Concrete.UniverseExpression cUniverse(int level, int truncated) {
    return new Concrete.UniverseExpression(POSITION, new Universe.Type(level, truncated));
  }

  public static List<Concrete.Pattern> cPatterns(Concrete.Pattern... patterns) {
    return Arrays.asList(patterns);
  }

  public static Concrete.ConstructorPattern cConPattern(String name, List<Concrete.PatternArgument> patternArgs) {
    return new Concrete.ConstructorPattern(POSITION, name, patternArgs);
  }

  public static Concrete.ConstructorPattern cConPattern(String name, Concrete.PatternArgument... patternArgs) {
    return cConPattern(name, Arrays.asList(patternArgs));
  }

  public static Concrete.NamePattern cNamePattern(String name) {
    return new Concrete.NamePattern(POSITION, name);
  }

  public static Concrete.PatternArgument cPatternArg(Concrete.Pattern pattern, boolean isExplicit, boolean isHidden) {
    return new Concrete.PatternArgument(POSITION, pattern, isExplicit, isHidden);
  }

  public static Concrete.BinOpExpression cBinOp(Concrete.Expression left, Definition binOp, Concrete.Expression right) {
    return new Concrete.BinOpExpression(POSITION, left, binOp, right);
  }

  public static Concrete.NumericLiteral cNum(int num) {
    return new Concrete.NumericLiteral(POSITION, num);
  }
}
