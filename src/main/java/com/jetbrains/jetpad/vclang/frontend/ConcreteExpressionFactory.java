package com.jetbrains.jetpad.vclang.frontend;

import com.jetbrains.jetpad.vclang.frontend.parser.Position;
import com.jetbrains.jetpad.vclang.frontend.reference.LocalReference;
import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.module.source.SourceId;
import com.jetbrains.jetpad.vclang.naming.reference.Referable;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.Prelude;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ConcreteExpressionFactory {
  private static final SourceId SOURCE_ID = new SourceId() {
    @Override public ModulePath getModulePath() { return ModulePath.moduleName(toString()); }
    @Override public String toString() { return "$transient$"; }
  };
  public static final Position POSITION = new Position(SOURCE_ID, 0, 0);

  public static Concrete.LamExpression<Position> cLam(List<Concrete.Parameter<Position>> arguments, Concrete.Expression<Position> body) {
    return new Concrete.LamExpression<>(POSITION, arguments, body);
  }

  public static Concrete.LamExpression<Position> cLam(Concrete.NameParameter<Position> var, Concrete.Expression<Position> body) {
    return cLam(Collections.singletonList(var), body);
  }

  public static Concrete.ReferenceExpression<Position> cVar(Referable referable) {
    return new Concrete.ReferenceExpression<>(POSITION, referable);
  }

  public static Concrete.ReferenceExpression<Position> cDefCall(Concrete.Expression<Position> expr, Referable referable) {
    return new Concrete.ReferenceExpression<>(POSITION, expr, referable);
  }

  public static Concrete.ReferenceExpression<Position> cDefCall(Concrete.Definition definition) {
    return new Concrete.ReferenceExpression<>(POSITION, definition);
  }

  public static Concrete.ClassExtExpression<Position> cClassExt(Concrete.Expression<Position> expr, List<Concrete.ClassFieldImpl<Position>> definitions) {
    return new Concrete.ClassExtExpression<>(POSITION, expr, definitions);
  }

  public static Concrete.ClassFieldImpl<Position> cImplStatement(Referable referable, Concrete.Expression<Position> expr) {
    return new Concrete.ClassFieldImpl<>(POSITION, referable, expr);
  }

  @SafeVarargs
  public static Concrete.Expression<Position> cApps(Concrete.Expression<Position> expr, Concrete.Expression<Position>... exprs) {
    for (Concrete.Expression<Position> expr1 : exprs) {
      expr = new Concrete.AppExpression<>(POSITION, expr, new Concrete.Argument<>(expr1, true));
    }
    return expr;
  }

  public static Concrete.Expression<Position> cApps(Concrete.Expression<Position> expr, Concrete.Expression<Position> arg, boolean explicit) {
    return new Concrete.AppExpression<>(POSITION, expr, new Concrete.Argument<>(arg, explicit));
  }

  public static Concrete.ReferenceExpression<Position> cNat() {
    return new Concrete.ReferenceExpression<>(POSITION, Prelude.NAT.getConcreteDefinition());
  }

  public static Concrete.ReferenceExpression<Position> cZero() {
    return new Concrete.ReferenceExpression<>(POSITION, Prelude.ZERO.getConcreteDefinition());
  }

  public static Concrete.ReferenceExpression<Position> cSuc() {
    return new Concrete.ReferenceExpression<>(POSITION, Prelude.SUC.getConcreteDefinition());
  }

  public static Concrete.LetExpression<Position> cLet(List<Concrete.LetClause<Position>> clauses, Concrete.Expression<Position> expr) {
    return new Concrete.LetExpression<>(POSITION, clauses, expr);
  }

  @SafeVarargs
  public static List<Concrete.LetClause<Position>> clets(Concrete.LetClause<Position>... letClauses) {
    return Arrays.asList(letClauses);
  }

  public static Concrete.LetClause<Position> clet(Referable referable, Concrete.Expression<Position> term) {
    return new Concrete.LetClause<>(POSITION, referable, Collections.emptyList(), null, term);
  }

  public static Concrete.LetClause<Position> clet(Referable referable, List<Concrete.Parameter<Position>> args, Concrete.Expression<Position> term) {
    return new Concrete.LetClause<>(POSITION, referable, args, null, term);
  }

  public static Concrete.LetClause<Position> clet(Referable referable, List<Concrete.Parameter<Position>> args, Concrete.Expression<Position> resultType, Concrete.Expression<Position> term) {
    return new Concrete.LetClause<>(POSITION, referable, args, resultType, term);
  }

  public static LocalReference ref(String name) {
    return new LocalReference(name);
  }

  public static List<Referable> cvars(Referable... vars) {
    return Arrays.asList(vars);
  }

  @SafeVarargs
  public static List<Concrete.Parameter<Position>> cargs(Concrete.Parameter<Position>... args) {
    return Arrays.asList(args);
  }

  @SafeVarargs
  public static List<Concrete.TypeParameter<Position>> ctypeArgs(Concrete.TypeParameter<Position>... args) {
    return Arrays.asList(args);
  }

  public static Concrete.NameParameter<Position> cName(String name) {
    return new Concrete.NameParameter<>(POSITION, true, name);
  }

  public static Concrete.NameParameter<Position> cName(boolean explicit, String name) {
    return new Concrete.NameParameter<>(POSITION, explicit, name);
  }

  public static Concrete.TypeParameter<Position> cTypeArg(boolean explicit, Concrete.Expression<Position> type) {
    return new Concrete.TypeParameter<>(explicit, type);
  }

  public static Concrete.TypeParameter<Position> cTypeArg(Concrete.Expression<Position> type) {
    return new Concrete.TypeParameter<>(true, type);
  }

  public static Concrete.TelescopeParameter<Position> cTele(List<Referable> referableList, Concrete.Expression<Position> type) {
    return new Concrete.TelescopeParameter<>(POSITION, true, referableList, type);
  }

  public static Concrete.TelescopeParameter<Position> cTele(boolean explicit, List<? extends Referable> referableList, Concrete.Expression<Position> type) {
    return new Concrete.TelescopeParameter<>(POSITION, explicit, referableList, type);
  }

  public static Concrete.PiExpression<Position> cPi(Concrete.Expression<Position> domain, Concrete.Expression<Position> codomain) {
    return new Concrete.PiExpression<>(POSITION, ctypeArgs(cTypeArg(domain)), codomain);
  }

  public static Concrete.Expression<Position> cPi(List<Concrete.TypeParameter<Position>> arguments, Concrete.Expression<Position> codomain) {
    return arguments.isEmpty() ? codomain : new Concrete.PiExpression<>(POSITION, arguments, codomain);
  }

  public static Concrete.PiExpression<Position> cPi(boolean explicit, Referable var, Concrete.Expression<Position> domain, Concrete.Expression<Position> codomain) {
    return new Concrete.PiExpression<>(POSITION, ctypeArgs(cTele(explicit, cvars(var), domain)), codomain);
  }

  public static Concrete.PiExpression<Position> cPi(Referable var, Concrete.Expression<Position> domain, Concrete.Expression<Position> codomain) {
    return cPi(true, var, domain, codomain);
  }

  public static Concrete.GoalExpression<Position> cGoal(String name, Concrete.Expression<Position> expression) {
    return new Concrete.GoalExpression<>(POSITION, name, expression);
  }

  public static Concrete.InferHoleExpression<Position> cInferHole() {
    return new Concrete.InferHoleExpression<>(POSITION);
  }

  public static Concrete.TupleExpression<Position> cTuple(List<Concrete.Expression<Position>> fields) {
    return new Concrete.TupleExpression<>(POSITION, fields);
  }

  public static Concrete.SigmaExpression<Position> cSigma(List<Concrete.TypeParameter<Position>> args) {
    return new Concrete.SigmaExpression<>(POSITION, args);
  }

  public static Concrete.ProjExpression<Position> cProj(Concrete.Expression<Position> expr, int field) {
    return new Concrete.ProjExpression<>(POSITION, expr, field);
  }

  public static Concrete.NewExpression<Position> cNew(Concrete.Expression<Position> expr) {
    return new Concrete.NewExpression<>(POSITION, expr);
  }

  public static Concrete.CaseExpression<Position> cCase(List<Concrete.Expression<Position>> expressions, List<Concrete.FunctionClause<Position>> clauses) {
    return new Concrete.CaseExpression<>(POSITION, expressions, clauses);
  }

  public static Concrete.FunctionClause<Position> cClause(List<Concrete.Pattern<Position>> patterns, Concrete.Expression<Position> expr) {
    return new Concrete.FunctionClause<>(POSITION, patterns, expr);
  }

  public static Concrete.UniverseExpression<Position> cUniverseInf(int level) {
    return new Concrete.UniverseExpression<>(POSITION, new Concrete.NumberLevelExpression<>(POSITION, level), new Concrete.InfLevelExpression<>(POSITION));
  }

  public static Concrete.UniverseExpression<Position> cUniverseStd(int level) {
    return new Concrete.UniverseExpression<>(POSITION, new Concrete.NumberLevelExpression<>(POSITION, level), new Concrete.HLevelExpression<>(POSITION));
  }

  public static Concrete.UniverseExpression<Position> cUniverse(Concrete.LevelExpression<Position> pLevel, Concrete.LevelExpression<Position> hLevel) {
    return new Concrete.UniverseExpression<>(POSITION, pLevel, hLevel);
  }

  public static Concrete.ConstructorPattern<Position> cConPattern(boolean isExplicit, Referable referable, List<Concrete.Pattern<Position>> patternArgs) {
    return new Concrete.ConstructorPattern<>(POSITION, isExplicit, referable, patternArgs);
  }

  public static Concrete.NamePattern<Position> cNamePattern(boolean isExplicit, String name) {
    return new Concrete.NamePattern<>(POSITION, isExplicit, name);
  }

  public static Concrete.EmptyPattern<Position> cEmptyPattern(boolean isExplicit) {
    return new Concrete.EmptyPattern<>(POSITION, isExplicit);
  }

  public static Concrete.BinOpExpression<Position> cBinOp(Concrete.Expression<Position> left, Referable binOp, Concrete.Expression<Position> right) {
    return new Concrete.BinOpExpression<>(POSITION, left, binOp, right);
  }

  public static Concrete.NumericLiteral<Position> cNum(int num) {
    return new Concrete.NumericLiteral<>(POSITION, num);
  }

  public static Concrete.TermFunctionBody<Position> body(Concrete.Expression<Position> term) {
    return new Concrete.TermFunctionBody<>(POSITION, term);
  }
}
