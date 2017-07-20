package com.jetbrains.jetpad.vclang.core.expr.factory;

import com.jetbrains.jetpad.vclang.core.context.binding.inference.InferenceLevelVariable;
import com.jetbrains.jetpad.vclang.core.context.binding.inference.InferenceVariable;
import com.jetbrains.jetpad.vclang.core.definition.ClassField;
import com.jetbrains.jetpad.vclang.frontend.Concrete;
import com.jetbrains.jetpad.vclang.term.Abstract;

import java.util.List;

import static com.jetbrains.jetpad.vclang.frontend.ConcreteExpressionFactory.*;

@SuppressWarnings("unchecked")
public class ConcreteExpressionFactory implements AbstractExpressionFactory {
  @Override
  public Abstract.Expression makeApp(Abstract.Expression fun, boolean explicit, Abstract.Expression arg) {
    return cApps((Concrete.Expression) fun, (Concrete.Expression) arg, explicit);
  }

  @Override
  public Abstract.Expression makeBinOp(Abstract.Expression left, Abstract.Definition defCall, Abstract.Expression right) {
    return cBinOp((Concrete.Expression) left, defCall, (Concrete.Expression) right);
  }

  @Override
  public Abstract.Expression makeDefCall(Abstract.Expression expr, Abstract.Definition definition) {
    return cDefCall((Concrete.Expression) expr, definition, definition == null ? "\\this" : definition.getName());
  }

  @Override
  public Abstract.Expression makeClassExt(Abstract.Expression expr, List<? extends Abstract.ClassFieldImpl> statements) {
    return cClassExt((Concrete.Expression) expr, (List<Concrete.ClassFieldImpl>) statements);
  }

  @Override
  public Abstract.ClassFieldImpl makeImplementStatement(ClassField field, Abstract.Expression type, Abstract.Expression term) {
    return cImplStatement(field.getName(), (Concrete.Expression) term);
  }

  @Override
  public Abstract.ReferableSourceNode makeReferable(String name) {
    return new Concrete.LocalVariable(POSITION, name);
  }

  @Override
  public Abstract.Expression makeVar(Abstract.ReferableSourceNode referable) {
    return cVar(referable);
  }

  @Override
  public Abstract.Expression makeInferVar(InferenceVariable variable) {
    return new Concrete.InferenceReferenceExpression(POSITION, variable);
  }

  @Override
  public Abstract.NameParameter makeNameParameter(boolean explicit, String name) {
    return cName(explicit, name);
  }

  @Override
  public Abstract.TypeParameter makeTypeParameter(boolean explicit, Abstract.Expression type) {
    return cTypeArg(explicit, (Concrete.Expression) type);
  }

  @Override
  public Abstract.TypeParameter makeTelescopeParameter(boolean explicit, List<? extends Abstract.ReferableSourceNode> referableList, Abstract.Expression type) {
    return cTele(explicit, referableList, (Concrete.Expression) type);
  }

  @Override
  public Abstract.Expression makeLam(List<? extends Abstract.Parameter> arguments, Abstract.Expression body) {
    return cLam((List<Concrete.Parameter>) arguments, (Concrete.Expression) body);
  }

  @Override
  public Abstract.Expression makePi(List<? extends Abstract.TypeParameter> arguments, Abstract.Expression codomain) {
    return cPi((List<Concrete.TypeParameter>) arguments, (Concrete.Expression) codomain);
  }

  @Override
  public Abstract.Expression makeUniverse(Abstract.LevelExpression pLevel, Abstract.LevelExpression hLevel) {
    return cUniverse((Concrete.LevelExpression) pLevel, (Concrete.LevelExpression) hLevel);
  }

  @Override
  public Abstract.LevelExpression makeInferVarLevel(InferenceLevelVariable variable) {
    return new Concrete.InferVarLevelExpression(variable);
  }

  @Override
  public Abstract.LevelExpression makePLevel() {
    return new Concrete.PLevelExpression(POSITION);
  }

  @Override
  public Abstract.LevelExpression makeHLevel() {
    return new Concrete.HLevelExpression(POSITION);
  }

  @Override
  public Abstract.LevelExpression makeNumberLevel(int number) {
    return new Concrete.NumberLevelExpression(POSITION, number);
  }

  @Override
  public Abstract.LevelExpression makeSucLevel(Abstract.LevelExpression expr) {
    return new Concrete.SucLevelExpression(POSITION, (Concrete.LevelExpression) expr);
  }

  @Override
  public Abstract.LevelExpression makeMaxLevel(Abstract.LevelExpression left, Abstract.LevelExpression right) {
    return new Concrete.MaxLevelExpression(POSITION, (Concrete.LevelExpression) left, (Concrete.LevelExpression) right);
  }

  @Override
  public Abstract.LevelExpression makeInf() {
    return new Concrete.InfLevelExpression(POSITION);
  }

  @Override
  public Abstract.Expression makeInferHole() {
    return cInferHole();
  }

  @Override
  public Abstract.Expression makeError(Abstract.Expression expr) {
    return cError();
  }

  @Override
  public Abstract.Expression makeTuple(List<? extends Abstract.Expression> fields) {
    return cTuple((List<Concrete.Expression>) fields);
  }

  @Override
  public Abstract.Expression makeSigma(List<? extends Abstract.TypeParameter> arguments) {
    return cSigma((List<Concrete.TypeParameter>) arguments);
  }

  @Override
  public Abstract.Expression makeProj(Abstract.Expression expr, int field) {
    return cProj((Concrete.Expression) expr, field);
  }

  @Override
  public Abstract.Expression makeNew(Abstract.Expression expr) {
    return cNew((Concrete.Expression) expr);
  }

  @Override
  public Abstract.Expression makeNumericalLiteral(int num) {
    return cNum(num);
  }

  @Override
  public Abstract.Expression makeLet(List<? extends Abstract.LetClause> clauses, Abstract.Expression expr) {
    return cLet((List<Concrete.LetClause>) clauses, (Concrete.Expression) expr);
  }

  @Override
  public Abstract.LetClause makeLetClause(Abstract.ReferableSourceNode referable, List<? extends Abstract.Parameter> arguments, Abstract.Expression resultType, Abstract.Expression term) {
    return clet(referable.getName(), (List<Concrete.Parameter>) arguments, (Concrete.Expression) resultType, (Concrete.Expression) term);
  }

  @Override
  public Abstract.Expression makeCase(List<? extends Abstract.Expression> expressions, List<? extends Abstract.FunctionClause> clauses) {
    return cCase((List<Concrete.Expression>) expressions, (List<Concrete.FunctionClause>) clauses);
  }

  @Override
  public Abstract.FunctionClause makeClause(List<? extends Abstract.Pattern> patterns, Abstract.Expression expr) {
    return cClause((List<Concrete.Pattern>) patterns, (Concrete.Expression) expr);
  }

  @Override
  public Abstract.Pattern makeConPattern(boolean isExplicit, Abstract.Constructor constructor, List<? extends Abstract.Pattern> args) {
    return cConPattern(isExplicit, constructor.getName(), (List<Concrete.Pattern>) args);
  }

  @Override
  public Abstract.Pattern makeNamePattern(boolean isExplicit, String name) {
    return cNamePattern(isExplicit, name);
  }

  @Override
  public Abstract.Pattern makeEmptyPattern(boolean isExplicit) {
    return cEmptyPattern(isExplicit);
  }
}
