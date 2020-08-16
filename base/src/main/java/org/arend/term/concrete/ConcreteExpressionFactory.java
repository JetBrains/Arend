package org.arend.term.concrete;

import org.arend.ext.module.LongName;
import org.arend.naming.reference.LocalReferable;
import org.arend.naming.reference.Referable;
import org.arend.prelude.Prelude;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ConcreteExpressionFactory {
  public static Concrete.LamExpression cLam(List<Concrete.Parameter> arguments, Concrete.Expression body) {
    return new Concrete.LamExpression(null, arguments, body);
  }

  public static Concrete.ApplyHoleExpression cAppHole() {
    return new Concrete.ApplyHoleExpression(null);
  }

  public static Concrete.LamExpression cLam(Concrete.NameParameter var, Concrete.Expression body) {
    return cLam(Collections.singletonList(var), body);
  }

  public static Concrete.ReferenceExpression cVar(Referable referable) {
    return new Concrete.ReferenceExpression(null, referable);
  }

  public static Concrete.ReferenceExpression cVar(LongName longName, Referable referable) {
    return longName == null
      ? new Concrete.ReferenceExpression(null, referable)
      : new Concrete.LongReferenceExpression(null, longName, referable);
  }

  public static Concrete.ReferenceExpression cDefCall(LongName longName, Referable referable, Concrete.LevelExpression level1, Concrete.LevelExpression level2) {
    return longName == null
      ? new Concrete.ReferenceExpression(null, referable, level1, level2)
      : new Concrete.LongReferenceExpression(null, longName, referable, level1, level2);
  }

  public static Concrete.ClassExtExpression cClassExt(Concrete.Expression expr, List<Concrete.ClassFieldImpl> definitions) {
    return Concrete.ClassExtExpression.make(null, expr, new Concrete.Coclauses(null, definitions));
  }

  public static Concrete.ClassFieldImpl cImplStatement(Referable referable, Concrete.Expression expr) {
    return new Concrete.ClassFieldImpl(null, referable, expr, null);
  }

  public static Concrete.Expression cApps(Concrete.Expression expr, Concrete.Expression... exprs) {
    List<Concrete.Argument> args = new ArrayList<>(exprs.length);
    for (Concrete.Expression expr1 : exprs) {
      args.add(new Concrete.Argument(expr1, true));
    }
    return Concrete.AppExpression.make(null, expr, args);
  }

  public static Concrete.ReferenceExpression cNat() {
    return new Concrete.ReferenceExpression(null, Prelude.NAT.getReferable());
  }

  public static Concrete.NumericLiteral cZero() {
    return new Concrete.NumericLiteral(null, BigInteger.valueOf(0));
  }

  public static Concrete.ReferenceExpression cSuc() {
    return new Concrete.ReferenceExpression(null, Prelude.SUC.getReferable());
  }

  public static Concrete.LetExpression cLet(List<Concrete.LetClause> clauses, Concrete.Expression expr) {
    return new Concrete.LetExpression(null, false, clauses, expr);
  }

  public static List<Concrete.LetClause> clets(Concrete.LetClause... letClauses) {
    return Arrays.asList(letClauses);
  }

  public static Concrete.LetClause clet(Referable referable, Concrete.Expression term) {
    return new Concrete.LetClause(referable, Collections.emptyList(), null, term);
  }

  public static Concrete.LetClause clet(Referable referable, List<Concrete.Parameter> args, Concrete.Expression term) {
    return new Concrete.LetClause(referable, args, null, term);
  }

  public static Concrete.LetClause clet(Referable referable, List<Concrete.Parameter> args, Concrete.Expression resultType, Concrete.Expression term) {
    return new Concrete.LetClause(referable, args, resultType, term);
  }

  public static LocalReferable ref(String name) {
    return new LocalReferable(name);
  }

  public static List<Referable> cvars(Referable... vars) {
    return Arrays.asList(vars);
  }

  public static List<Concrete.Parameter> cargs(Concrete.Parameter... args) {
    return Arrays.asList(args);
  }

  public static List<Concrete.TelescopeParameter> cTeleArgs(Concrete.TelescopeParameter... args) {
    return Arrays.asList(args);
  }

  public static List<Concrete.TypeParameter> ctypeArgs(Concrete.TypeParameter... args) {
    return Arrays.asList(args);
  }

  public static Concrete.NameParameter cName(Referable referable) {
    return new Concrete.NameParameter(null, true, referable);
  }

  public static Concrete.NameParameter cName(boolean explicit, Referable referable) {
    return new Concrete.NameParameter(null, explicit, referable);
  }

  public static Concrete.TypeParameter cTypeArg(boolean explicit, Concrete.Expression type) {
    return new Concrete.TypeParameter(explicit, type);
  }

  public static Concrete.TypeParameter cTypeArg(Concrete.Expression type) {
    return new Concrete.TypeParameter(true, type);
  }

  public static Concrete.TelescopeParameter cTele(List<Referable> referableList, Concrete.Expression type) {
    return new Concrete.TelescopeParameter(null, true, referableList, type);
  }

  public static Concrete.TelescopeParameter cTele(boolean explicit, List<? extends Referable> referableList, Concrete.Expression type) {
    return new Concrete.TelescopeParameter(null, explicit, referableList, type);
  }

  public static Concrete.PiExpression cPi(Concrete.Expression domain, Concrete.Expression codomain) {
    return new Concrete.PiExpression(null, ctypeArgs(cTypeArg(domain)), codomain);
  }

  public static Concrete.Expression cPi(List<Concrete.TypeParameter> arguments, Concrete.Expression codomain) {
    return arguments.isEmpty() ? codomain : new Concrete.PiExpression(null, arguments, codomain);
  }

  public static Concrete.PiExpression cPi(boolean explicit, Referable var, Concrete.Expression domain, Concrete.Expression codomain) {
    return new Concrete.PiExpression(null, ctypeArgs(cTele(explicit, cvars(var), domain)), codomain);
  }

  public static Concrete.PiExpression cPi(Referable var, Concrete.Expression domain, Concrete.Expression codomain) {
    return cPi(true, var, domain, codomain);
  }

  public static Concrete.GoalExpression cGoal(String name, Concrete.Expression expression) {
    return new Concrete.GoalExpression(null, name, expression);
  }

  public static Concrete.HoleExpression cInferHole() {
    return new Concrete.HoleExpression(null);
  }

  public static Concrete.TupleExpression cTuple(List<Concrete.Expression> fields) {
    return new Concrete.TupleExpression(null, fields);
  }

  public static Concrete.SigmaExpression cSigma(List<Concrete.TypeParameter> args) {
    return new Concrete.SigmaExpression(null, args);
  }

  public static Concrete.ProjExpression cProj(Concrete.Expression expr, int field) {
    return new Concrete.ProjExpression(null, expr, field);
  }

  public static Concrete.NewExpression cNew(Concrete.Expression expr) {
    return new Concrete.NewExpression(null, expr);
  }

  public static Concrete.EvalExpression cEval(boolean isPEval, Concrete.Expression expr) {
    return new Concrete.EvalExpression(null, isPEval, expr);
  }

  public static Concrete.CaseArgument cCaseArg(Concrete.Expression expression, Referable referable, Concrete.Expression type) {
    return new Concrete.CaseArgument(expression, referable, type);
  }

  public static Concrete.CaseExpression cCase(boolean isSFunc, List<Concrete.CaseArgument> arguments, Concrete.Expression resultType, Concrete.Expression resultTypeLevel, List<Concrete.FunctionClause> clauses) {
    return new Concrete.CaseExpression(null, isSFunc, arguments, resultType, resultTypeLevel, clauses);
  }

  public static Concrete.FunctionClause cClause(List<Concrete.Pattern> patterns, Concrete.Expression expr) {
    return new Concrete.FunctionClause(null, patterns, expr);
  }

  public static Concrete.UniverseExpression cUniverseInf(int level) {
    return new Concrete.UniverseExpression(null, new Concrete.NumberLevelExpression(null, level), new Concrete.InfLevelExpression(null));
  }

  public static Concrete.UniverseExpression cUniverseStd(int level) {
    return new Concrete.UniverseExpression(null, new Concrete.NumberLevelExpression(null, level), new Concrete.HLevelExpression(null));
  }

  public static Concrete.UniverseExpression cUniverse(Concrete.LevelExpression pLevel, Concrete.LevelExpression hLevel) {
    return new Concrete.UniverseExpression(null, pLevel, hLevel);
  }

  public static Concrete.ConstructorPattern cConPattern(boolean isExplicit, Referable referable, List<Concrete.Pattern> patternArgs) {
    return new Concrete.ConstructorPattern(null, isExplicit, referable, patternArgs, Collections.emptyList());
  }

  public static Concrete.TuplePattern cTuplePattern(boolean isExplicit, List<Concrete.Pattern> patternArgs) {
    return new Concrete.TuplePattern(null, isExplicit, patternArgs, Collections.emptyList());
  }

  public static Concrete.NamePattern cNamePattern(boolean isExplicit, Referable referable) {
    return new Concrete.NamePattern(null, isExplicit, referable, null);
  }

  public static Concrete.TuplePattern cEmptyPattern(boolean isExplicit) {
    return new Concrete.TuplePattern(null, isExplicit, Collections.emptyList(), Collections.emptyList());
  }

  public static Concrete.Expression cBinOp(Concrete.Expression left, Referable binOp, Concrete.Expression implicit, Concrete.Expression right) {
    List<Concrete.Argument> args = new ArrayList<>(3);
    if (implicit != null) {
      args.add(new Concrete.Argument(implicit, false));
    }
    args.add(new Concrete.Argument(left, true));
    args.add(new Concrete.Argument(right, true));
    return Concrete.AppExpression.make(null, new Concrete.ReferenceExpression(null, binOp), args);
  }

  public static Concrete.NumericLiteral cNum(BigInteger num) {
    return new Concrete.NumericLiteral(null, num);
  }

  public static Concrete.NumericLiteral cNum(long num) {
    return new Concrete.NumericLiteral(null, BigInteger.valueOf(num));
  }

  public static Concrete.TermFunctionBody body(Concrete.Expression term) {
    return new Concrete.TermFunctionBody(null, term);
  }
}
