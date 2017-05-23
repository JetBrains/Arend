package com.jetbrains.jetpad.vclang.frontend;

import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.module.source.SourceId;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Prelude;

import java.util.Arrays;
import java.util.List;

public class ConcreteExpressionFactory {
  private static final SourceId SOURCE_ID = new SourceId() {
    @Override public ModulePath getModulePath() { return ModulePath.moduleName(toString()); }
    @Override public String toString() { return "$transient$"; }
  };
  public static final Concrete.Position POSITION = new Concrete.Position(SOURCE_ID, 0, 0);

  public static Concrete.LamExpression cLam(List<Concrete.Argument> arguments, Concrete.Expression body) {
    return new Concrete.LamExpression(POSITION, arguments, body);
  }

  public static Concrete.LamExpression cLam(Concrete.ReferableSourceNode var, Concrete.Expression body) {
    return cLam(cargs(cName(var)), body);
  }

  public static Concrete.ReferenceExpression cVar(Abstract.ReferableSourceNode referable) {
    return new Concrete.ReferenceExpression(POSITION, referable);
  }

  public static Concrete.ReferenceExpression cDefCall(Concrete.Expression expr, Abstract.Definition definition, String name) {
    Concrete.ReferenceExpression result = new Concrete.ReferenceExpression(POSITION, expr, name);
    result.setResolvedReferent(definition);
    return result;
  }

  public static Concrete.ReferenceExpression cDefCall(Abstract.Definition definition) {
    return cDefCall(null, definition, definition.getName());
  }

  public static Concrete.ClassExtExpression cClassExt(Concrete.Expression expr, List<Concrete.ClassFieldImpl> definitions) {
    return new Concrete.ClassExtExpression(POSITION, expr, definitions);
  }

  public static Concrete.ClassFieldImpl cImplStatement(String name, Concrete.Expression expr) {
    return new Concrete.ClassFieldImpl(POSITION, name, expr);
  }

  public static Concrete.Expression cApps(Concrete.Expression expr, Concrete.Expression... exprs) {
    for (Concrete.Expression expr1 : exprs) {
      expr = new Concrete.AppExpression(POSITION, expr, new Concrete.ArgumentExpression(expr1, true, false));
    }
    return expr;
  }

  public static Concrete.Expression cApps(Concrete.Expression expr, Concrete.Expression arg, boolean explicit) {
    return new Concrete.AppExpression(POSITION, expr, new Concrete.ArgumentExpression(arg, explicit, false));
  }

  public static Concrete.ReferenceExpression cNat() {
    return new Concrete.ReferenceExpression(POSITION, Prelude.NAT.getAbstractDefinition());
  }

  public static Concrete.ReferenceExpression cZero() {
    return new Concrete.ReferenceExpression(POSITION, Prelude.ZERO.getAbstractDefinition());
  }

  public static Concrete.ReferenceExpression cSuc() {
    return new Concrete.ReferenceExpression(POSITION, Prelude.SUC.getAbstractDefinition());
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

  public static Concrete.LetClause clet(String name, List<Concrete.Argument> args, Concrete.Expression resultType, Abstract.Definition.Arrow arrow, Concrete.Expression term) {
    return new Concrete.LetClause(POSITION, name, args, resultType, arrow, term);
  }

  public static Concrete.LocalVariable ref(String name) {
    return new Concrete.LocalVariable(POSITION, name);
  }

  public static List<Concrete.ReferableSourceNode> cvars(Concrete.ReferableSourceNode... vars) {
    return Arrays.asList(vars);
  }

  public static List<Concrete.Argument> cargs(Concrete.Argument... args) {
    return Arrays.asList(args);
  }

  public static List<Concrete.TypeArgument> ctypeArgs(Concrete.TypeArgument... args) {
    return Arrays.asList(args);
  }

  public static Concrete.NameArgument cName(Concrete.ReferableSourceNode referable) {
    return new Concrete.NameArgument(POSITION, true, referable);
  }

  public static Concrete.NameArgument cName(boolean explicit, Abstract.ReferableSourceNode referable) {
    return new Concrete.NameArgument(POSITION, explicit, referable);
  }

  public static Concrete.TypeArgument cTypeArg(boolean explicit, Concrete.Expression type) {
    return new Concrete.TypeArgument(explicit, type);
  }

  public static Concrete.TypeArgument cTypeArg(Concrete.Expression type) {
    return new Concrete.TypeArgument(true, type);
  }

  public static Concrete.TelescopeArgument cTele(List<Concrete.ReferableSourceNode> referableList, Concrete.Expression type) {
    return new Concrete.TelescopeArgument(POSITION, true, referableList, type);
  }

  public static Concrete.TelescopeArgument cTele(boolean explicit, List<? extends Abstract.ReferableSourceNode> referableList, Concrete.Expression type) {
    return new Concrete.TelescopeArgument(POSITION, explicit, referableList, type);
  }

  public static Concrete.PiExpression cPi(Concrete.Expression domain, Concrete.Expression codomain) {
    return new Concrete.PiExpression(POSITION, ctypeArgs(cTypeArg(domain)), codomain);
  }

  public static Concrete.Expression cPi(List<Concrete.TypeArgument> arguments, Concrete.Expression codomain) {
    return arguments.isEmpty() ? codomain : new Concrete.PiExpression(POSITION, arguments, codomain);
  }

  public static Concrete.PiExpression cPi(boolean explicit, Concrete.ReferableSourceNode var, Concrete.Expression domain, Concrete.Expression codomain) {
    return (Concrete.PiExpression) cPi(ctypeArgs(cTele(explicit, cvars(var), domain)), codomain);
  }

  public static Concrete.PiExpression cPi(Concrete.ReferableSourceNode var, Concrete.Expression domain, Concrete.Expression codomain) {
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

  public static Concrete.UniverseExpression cUniverseInf(int level) {
    return new Concrete.UniverseExpression(POSITION, new Concrete.NumberLevelExpression(POSITION, level), new Concrete.InfLevelExpression(POSITION));
  }

  public static Concrete.UniverseExpression cUniverseStd(int level) {
    return new Concrete.UniverseExpression(POSITION, new Concrete.NumberLevelExpression(POSITION, level), new Concrete.HLevelExpression(POSITION));
  }

  public static Concrete.UniverseExpression cUniverse(Concrete.LevelExpression pLevel, Concrete.LevelExpression hLevel) {
    return new Concrete.UniverseExpression(POSITION, pLevel, hLevel);
  }

  public static Concrete.InfLevelExpression cInf() {
    return new Concrete.InfLevelExpression(POSITION);
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

  public static Concrete.NamePattern cNamePattern(Abstract.ReferableSourceNode referable) {
    return new Concrete.NamePattern(POSITION, referable);
  }

  public static Concrete.PatternArgument cPatternArg(Concrete.Pattern pattern, boolean isExplicit) {
    return new Concrete.PatternArgument(POSITION, pattern, isExplicit);
  }

  public static Concrete.BinOpExpression cBinOp(Concrete.Expression left, Abstract.Definition binOp, Concrete.Expression right) {
    return new Concrete.BinOpExpression(POSITION, left, binOp, right);
  }

  public static Concrete.NumericLiteral cNum(int num) {
    return new Concrete.NumericLiteral(POSITION, num);
  }
}
