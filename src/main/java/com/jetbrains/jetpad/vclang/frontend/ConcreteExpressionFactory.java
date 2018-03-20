package com.jetbrains.jetpad.vclang.frontend;

import com.jetbrains.jetpad.vclang.frontend.reference.ParsedLocalReferable;
import com.jetbrains.jetpad.vclang.naming.reference.Referable;
import com.jetbrains.jetpad.vclang.prelude.Prelude;
import com.jetbrains.jetpad.vclang.term.Fixity;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ConcreteExpressionFactory {
  public static Concrete.LamExpression cLam(List<Concrete.Parameter> arguments, Concrete.Expression body) {
    return new Concrete.LamExpression(null, arguments, body);
  }

  public static Concrete.LamExpression cLam(Concrete.NameParameter var, Concrete.Expression body) {
    return cLam(Collections.singletonList(var), body);
  }

  public static Concrete.ReferenceExpression cVar(Referable referable) {
    return new Concrete.ReferenceExpression(null, referable);
  }

  public static Concrete.ReferenceExpression cDefCall(Referable referable, Concrete.LevelExpression level1, Concrete.LevelExpression level2) {
    return new Concrete.ReferenceExpression(null, referable, level1, level2);
  }

  public static Concrete.ClassExtExpression cClassExt(Concrete.Expression expr, List<Concrete.ClassFieldImpl> definitions) {
    return new Concrete.ClassExtExpression(null, expr, definitions);
  }

  public static Concrete.ClassFieldImpl cImplStatement(Referable referable, Concrete.Expression expr) {
    return new Concrete.ClassFieldImpl(null, referable, expr);
  }

  public static Concrete.Expression cApps(Concrete.Expression expr, Concrete.Expression... exprs) {
    for (Concrete.Expression expr1 : exprs) {
      expr = new Concrete.AppExpression(null, expr, new Concrete.Argument(expr1, true));
    }
    return expr;
  }

  public static Concrete.Expression cApps(Concrete.Expression expr, Concrete.Expression arg, boolean explicit) {
    return new Concrete.AppExpression(null, expr, new Concrete.Argument(arg, explicit));
  }

  public static Concrete.ReferenceExpression cNat() {
    return new Concrete.ReferenceExpression(null, Prelude.NAT.getReferable());
  }

  public static Concrete.ReferenceExpression cZero() {
    return new Concrete.ReferenceExpression(null, Prelude.ZERO.getReferable());
  }

  public static Concrete.ReferenceExpression cSuc() {
    return new Concrete.ReferenceExpression(null, Prelude.SUC.getReferable());
  }

  public static Concrete.LetExpression cLet(List<Concrete.LetClause> clauses, Concrete.Expression expr) {
    return new Concrete.LetExpression(null, clauses, expr);
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

  public static ParsedLocalReferable ref(String name) {
    return new ParsedLocalReferable(null, name);
  }

  public static List<Referable> cvars(Referable... vars) {
    return Arrays.asList(vars);
  }

  public static List<Concrete.Parameter> cargs(Concrete.Parameter... args) {
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

  public static Concrete.InferHoleExpression cInferHole() {
    return new Concrete.InferHoleExpression(null);
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

  public static Concrete.CaseExpression cCase(List<Concrete.Expression> expressions, List<Concrete.FunctionClause> clauses) {
    return new Concrete.CaseExpression(null, expressions, clauses);
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
    return new Concrete.ConstructorPattern(null, isExplicit, referable, patternArgs);
  }

  public static Concrete.NamePattern cNamePattern(boolean isExplicit, Referable referable) {
    return new Concrete.NamePattern(null, isExplicit, referable);
  }

  public static Concrete.EmptyPattern cEmptyPattern(boolean isExplicit) {
    return new Concrete.EmptyPattern(null, isExplicit);
  }

  public static Concrete.Expression cBinOp(Concrete.Expression left, Referable binOp, Concrete.Expression right) {
    List<Concrete.BinOpSequenceElem> sequence = new ArrayList<>(3);
    sequence.add(new Concrete.BinOpSequenceElem(left, Fixity.NONFIX, true));
    sequence.add(new Concrete.BinOpSequenceElem(new Concrete.ReferenceExpression(null, binOp), Fixity.UNKNOWN, true));
    sequence.add(new Concrete.BinOpSequenceElem(right, Fixity.NONFIX, true));
    return new Concrete.BinOpSequenceExpression(null, sequence);
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
