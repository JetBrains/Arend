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

  public static Concrete.LamExpression cLam(List<Concrete.Parameter> arguments, Concrete.Expression body) {
    return new Concrete.LamExpression(POSITION, arguments, body);
  }

  public static Concrete.LamExpression cLam(Concrete.NameParameter var, Concrete.Expression body) {
    return cLam(cargs(var), body);
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
      expr = new Concrete.AppExpression(POSITION, expr, new Concrete.Argument(expr1, true));
    }
    return expr;
  }

  public static Concrete.Expression cApps(Concrete.Expression expr, Concrete.Expression arg, boolean explicit) {
    return new Concrete.AppExpression(POSITION, expr, new Concrete.Argument(arg, explicit));
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

  public static Concrete.LetExpression cLet(List<Concrete.LetClause> clauses, Concrete.Expression expr) {
    return new Concrete.LetExpression(POSITION, clauses, expr);
  }

  public static List<Concrete.LetClause> clets(Concrete.LetClause... letClauses) {
    return Arrays.asList(letClauses);
  }

  public static Concrete.LetClause clet(String name, Concrete.Expression term) {
    return new Concrete.LetClause(POSITION, name, cargs(), null, term);
  }

  public static Concrete.LetClause clet(String name, List<Concrete.Parameter> args, Concrete.Expression term) {
    return new Concrete.LetClause(POSITION, name, args, null, term);
  }

  public static Concrete.LetClause clet(String name, List<Concrete.Parameter> args, Concrete.Expression resultType, Concrete.Expression term) {
    return new Concrete.LetClause(POSITION, name, args, resultType, term);
  }

  public static Concrete.LocalVariable ref(String name) {
    return new Concrete.LocalVariable(POSITION, name);
  }

  public static List<Abstract.ReferableSourceNode> cvars(Abstract.ReferableSourceNode... vars) {
    return Arrays.asList(vars);
  }

  public static List<Concrete.Parameter> cargs(Concrete.Parameter... args) {
    return Arrays.asList(args);
  }

  public static List<Concrete.TypeParameter> ctypeArgs(Concrete.TypeParameter... args) {
    return Arrays.asList(args);
  }

  public static Concrete.NameParameter cName(String name) {
    return new Concrete.NameParameter(POSITION, true, name);
  }

  public static Concrete.NameParameter cName(boolean explicit, String name) {
    return new Concrete.NameParameter(POSITION, explicit, name);
  }

  public static Concrete.TypeParameter cTypeArg(boolean explicit, Concrete.Expression type) {
    return new Concrete.TypeParameter(explicit, type);
  }

  public static Concrete.TypeParameter cTypeArg(Concrete.Expression type) {
    return new Concrete.TypeParameter(true, type);
  }

  public static Concrete.TelescopeParameter cTele(List<Abstract.ReferableSourceNode> referableList, Concrete.Expression type) {
    return new Concrete.TelescopeParameter(POSITION, true, referableList, type);
  }

  public static Concrete.TelescopeParameter cTele(boolean explicit, List<? extends Abstract.ReferableSourceNode> referableList, Concrete.Expression type) {
    return new Concrete.TelescopeParameter(POSITION, explicit, referableList, type);
  }

  public static Concrete.PiExpression cPi(Concrete.Expression domain, Concrete.Expression codomain) {
    return new Concrete.PiExpression(POSITION, ctypeArgs(cTypeArg(domain)), codomain);
  }

  public static Concrete.Expression cPi(List<Concrete.TypeParameter> arguments, Concrete.Expression codomain) {
    return arguments.isEmpty() ? codomain : new Concrete.PiExpression(POSITION, arguments, codomain);
  }

  public static Concrete.PiExpression cPi(boolean explicit, Abstract.ReferableSourceNode var, Concrete.Expression domain, Concrete.Expression codomain) {
    return (Concrete.PiExpression) cPi(ctypeArgs(cTele(explicit, cvars(var), domain)), codomain);
  }

  public static Concrete.PiExpression cPi(Abstract.ReferableSourceNode var, Concrete.Expression domain, Concrete.Expression codomain) {
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

  public static Concrete.SigmaExpression cSigma(List<Concrete.TypeParameter> args) {
    return new Concrete.SigmaExpression(POSITION, args);
  }

  public static Concrete.ProjExpression cProj(Concrete.Expression expr, int field) {
    return new Concrete.ProjExpression(POSITION, expr, field);
  }

  public static Concrete.NewExpression cNew(Concrete.Expression expr) {
    return new Concrete.NewExpression(POSITION, expr);
  }

  public static Concrete.CaseExpression cCase(List<Concrete.Expression> expressions, List<Concrete.FunctionClause> clauses) {
    return new Concrete.CaseExpression(POSITION, expressions, clauses);
  }

  public static Concrete.FunctionClause cClause(List<Concrete.Pattern> patterns, Concrete.Expression expr) {
    return new Concrete.FunctionClause(POSITION, patterns, expr);
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

  public static Concrete.ConstructorPattern cConPattern(boolean isExplicit, String name, List<Concrete.Pattern> patternArgs) {
    return new Concrete.ConstructorPattern(POSITION, isExplicit, name, patternArgs);
  }

  public static Concrete.NamePattern cNamePattern(boolean isExplicit, String name) {
    return new Concrete.NamePattern(POSITION, isExplicit, name);
  }

  public static Concrete.EmptyPattern cEmptyPattern(boolean isExplicit) {
    return new Concrete.EmptyPattern(POSITION, isExplicit);
  }

  public static Concrete.BinOpExpression cBinOp(Concrete.Expression left, Abstract.Definition binOp, Concrete.Expression right) {
    return new Concrete.BinOpExpression(POSITION, left, binOp, right);
  }

  public static Concrete.NumericLiteral cNum(int num) {
    return new Concrete.NumericLiteral(POSITION, num);
  }

  public static Concrete.TermFunctionBody body(Concrete.Expression term) {
    return new Concrete.TermFunctionBody(POSITION, term);
  }
}
