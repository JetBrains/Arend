package com.jetbrains.jetpad.vclang.core.expr.factory;

import com.jetbrains.jetpad.vclang.core.context.binding.inference.InferenceLevelVariable;
import com.jetbrains.jetpad.vclang.core.context.binding.inference.InferenceVariable;
import com.jetbrains.jetpad.vclang.core.definition.ClassField;
import com.jetbrains.jetpad.vclang.frontend.Concrete;
import com.jetbrains.jetpad.vclang.term.Abstract;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

import static com.jetbrains.jetpad.vclang.frontend.ConcreteExpressionFactory.*;

@SuppressWarnings("unchecked")
public class ConcreteExpressionFactory implements AbstractExpressionFactory {
  @Nonnull
  @Override
  public Abstract.Expression makeApp(@Nonnull Abstract.Expression fun, boolean explicit, @Nonnull Abstract.Expression arg) {
    return cApps((Concrete.Expression) fun, (Concrete.Expression) arg, explicit);
  }

  @Nonnull
  @Override
  public Abstract.Expression makeBinOp(@Nonnull Abstract.Expression left, @Nonnull Abstract.Definition defCall, @Nonnull Abstract.Expression right) {
    return cBinOp((Concrete.Expression) left, defCall, (Concrete.Expression) right);
  }

  @Nonnull
  @Override
  public Abstract.Expression makeDefCall(@Nullable Abstract.Expression expr, @Nullable Abstract.Definition definition) {
    return cDefCall((Concrete.Expression) expr, definition, definition == null ? "\\this" : definition.getName());
  }

  @Nonnull
  @Override
  public Abstract.Expression makeClassExt(@Nonnull Abstract.Expression expr, @Nonnull List<? extends Abstract.ClassFieldImpl> statements) {
    return cClassExt((Concrete.Expression) expr, (List<Concrete.ClassFieldImpl>) statements);
  }

  @Nonnull
  @Override
  public Abstract.ClassFieldImpl makeImplementStatement(@Nonnull ClassField field, @Nonnull Abstract.Expression term) {
    return cImplStatement(field.getName(), (Concrete.Expression) term);
  }

  @Nonnull
  @Override
  public Abstract.ReferableSourceNode makeReferable(@Nullable String name) {
    return new Concrete.LocalVariable(POSITION, name);
  }

  @Nonnull
  @Override
  public Abstract.Expression makeVar(@Nonnull Abstract.ReferableSourceNode referable) {
    return cVar(referable);
  }

  @Nonnull
  @Override
  public Abstract.Expression makeInferVar(@Nonnull InferenceVariable variable) {
    return new Concrete.InferenceReferenceExpression(POSITION, variable);
  }

  @Nonnull
  @Override
  public Abstract.NameParameter makeNameParameter(boolean explicit, @Nullable String name) {
    return cName(explicit, name);
  }

  @Nonnull
  @Override
  public Abstract.TypeParameter makeTypeParameter(boolean explicit, @Nonnull Abstract.Expression type) {
    return cTypeArg(explicit, (Concrete.Expression) type);
  }

  @Nonnull
  @Override
  public Abstract.TypeParameter makeTelescopeParameter(boolean explicit, @Nonnull List<? extends Abstract.ReferableSourceNode> referableList, @Nonnull Abstract.Expression type) {
    return cTele(explicit, referableList, (Concrete.Expression) type);
  }

  @Nonnull
  @Override
  public Abstract.Expression makeLam(@Nonnull List<? extends Abstract.Parameter> arguments, @Nonnull Abstract.Expression body) {
    return cLam((List<Concrete.Parameter>) arguments, (Concrete.Expression) body);
  }

  @Nonnull
  @Override
  public Abstract.Expression makePi(@Nonnull List<? extends Abstract.TypeParameter> arguments, @Nonnull Abstract.Expression codomain) {
    return cPi((List<Concrete.TypeParameter>) arguments, (Concrete.Expression) codomain);
  }

  @Nonnull
  @Override
  public Abstract.Expression makeUniverse(@Nullable Abstract.LevelExpression pLevel, @Nullable Abstract.LevelExpression hLevel) {
    return cUniverse((Concrete.LevelExpression) pLevel, (Concrete.LevelExpression) hLevel);
  }

  @Nonnull
  @Override
  public Abstract.LevelExpression makeInferVarLevel(@Nonnull InferenceLevelVariable variable) {
    return new Concrete.InferVarLevelExpression(variable);
  }

  @Nonnull
  @Override
  public Abstract.LevelExpression makePLevel() {
    return new Concrete.PLevelExpression(POSITION);
  }

  @Nonnull
  @Override
  public Abstract.LevelExpression makeHLevel() {
    return new Concrete.HLevelExpression(POSITION);
  }

  @Nonnull
  @Override
  public Abstract.LevelExpression makeNumberLevel(int number) {
    return new Concrete.NumberLevelExpression(POSITION, number);
  }

  @Nonnull
  @Override
  public Abstract.LevelExpression makeSucLevel(@Nonnull Abstract.LevelExpression expr) {
    return new Concrete.SucLevelExpression(POSITION, (Concrete.LevelExpression) expr);
  }

  @Nonnull
  @Override
  public Abstract.LevelExpression makeMaxLevel(@Nonnull Abstract.LevelExpression left, @Nonnull Abstract.LevelExpression right) {
    return new Concrete.MaxLevelExpression(POSITION, (Concrete.LevelExpression) left, (Concrete.LevelExpression) right);
  }

  @Nonnull
  @Override
  public Abstract.LevelExpression makeInf() {
    return new Concrete.InfLevelExpression(POSITION);
  }

  @Nonnull
  @Override
  public Abstract.Expression makeInferHole() {
    return cInferHole();
  }

  @Nonnull
  @Override
  public Abstract.Expression makeError(Abstract.Expression expr) {
    return cError();
  }

  @Nonnull
  @Override
  public Abstract.Expression makeTuple(@Nonnull List<? extends Abstract.Expression> fields) {
    return cTuple((List<Concrete.Expression>) fields);
  }

  @Nonnull
  @Override
  public Abstract.Expression makeSigma(@Nonnull List<? extends Abstract.TypeParameter> arguments) {
    return cSigma((List<Concrete.TypeParameter>) arguments);
  }

  @Nonnull
  @Override
  public Abstract.Expression makeProj(@Nonnull Abstract.Expression expr, int field) {
    return cProj((Concrete.Expression) expr, field);
  }

  @Nonnull
  @Override
  public Abstract.Expression makeNew(@Nonnull Abstract.Expression expr) {
    return cNew((Concrete.Expression) expr);
  }

  @Nonnull
  @Override
  public Abstract.Expression makeNumericalLiteral(int num) {
    return cNum(num);
  }

  @Nonnull
  @Override
  public Abstract.Expression makeLet(@Nonnull List<? extends Abstract.LetClause> clauses, @Nonnull Abstract.Expression expr) {
    return cLet((List<Concrete.LetClause>) clauses, (Concrete.Expression) expr);
  }

  @Nonnull
  @Override
  public Abstract.LetClause makeLetClause(@Nonnull Abstract.ReferableSourceNode referable, @Nonnull List<? extends Abstract.Parameter> arguments, @Nonnull Abstract.Expression term) {
    return clet(referable.getName(), (List<Concrete.Parameter>) arguments, null, (Concrete.Expression) term);
  }

  @Nonnull
  @Override
  public Abstract.Expression makeCase(@Nonnull List<? extends Abstract.Expression> expressions, @Nonnull List<? extends Abstract.FunctionClause> clauses) {
    return cCase((List<Concrete.Expression>) expressions, (List<Concrete.FunctionClause>) clauses);
  }

  @Nonnull
  @Override
  public Abstract.FunctionClause makeClause(@Nonnull List<? extends Abstract.Pattern> patterns, @Nonnull Abstract.Expression expr) {
    return cClause((List<Concrete.Pattern>) patterns, (Concrete.Expression) expr);
  }

  @Nonnull
  @Override
  public Abstract.Pattern makeConPattern(boolean isExplicit, @Nonnull Abstract.Constructor constructor, @Nonnull List<? extends Abstract.Pattern> args) {
    return cConPattern(isExplicit, constructor.getName(), (List<Concrete.Pattern>) args);
  }

  @Nonnull
  @Override
  public Abstract.Pattern makeNamePattern(boolean isExplicit, @Nullable String name) {
    return cNamePattern(isExplicit, name);
  }

  @Nonnull
  @Override
  public Abstract.Pattern makeEmptyPattern(boolean isExplicit) {
    return cEmptyPattern(isExplicit);
  }
}
