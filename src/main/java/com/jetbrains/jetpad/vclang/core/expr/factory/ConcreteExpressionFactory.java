package com.jetbrains.jetpad.vclang.core.expr.factory;

import com.jetbrains.jetpad.vclang.core.context.binding.inference.InferenceLevelVariable;
import com.jetbrains.jetpad.vclang.core.context.binding.inference.InferenceVariable;
import com.jetbrains.jetpad.vclang.core.definition.ClassField;
import com.jetbrains.jetpad.vclang.frontend.text.Position;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Concrete;

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
  public Abstract.Expression makeBinOp(@Nonnull Abstract.Expression left, @Nonnull Abstract.ReferableSourceNode referable, @Nonnull Abstract.Expression right) {
    return cBinOp((Concrete.Expression) left, referable, (Concrete.Expression) right);
  }

  @Nonnull
  @Override
  public Abstract.Expression makeReference(@Nullable Abstract.Expression expr, @Nullable Abstract.ReferableSourceNode referable) {
    return cDefCall((Concrete.Expression) expr, referable, referable == null ? "\\this" : referable.getName());
  }

  @Nonnull
  @Override
  public Abstract.Expression makeClassExt(@Nonnull Abstract.Expression expr, @Nonnull List<? extends Abstract.ClassFieldImpl> statements) {
    return cClassExt((Concrete.Expression<Position>) expr, (List<Concrete.ClassFieldImpl<Position>>) statements);
  }

  @Nonnull
  @Override
  public Abstract.ClassFieldImpl makeImplementStatement(@Nonnull ClassField field, @Nonnull Abstract.Expression term) {
    return cImplStatement(field.getName(), (Concrete.Expression) term);
  }

  @Nonnull
  @Override
  public Abstract.ReferableSourceNode makeReferable(@Nullable String name) {
    return new Concrete.LocalVariable(null, name);
  }

  @Nonnull
  @Override
  public Abstract.Expression makeInferVar(@Nonnull InferenceVariable variable) {
    return new Concrete.InferenceReferenceExpression(null, variable);
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
    return cLam((List<Concrete.Parameter<Position>>) arguments, (Concrete.Expression) body);
  }

  @Nonnull
  @Override
  public Abstract.Expression makePi(@Nonnull List<? extends Abstract.TypeParameter> arguments, @Nonnull Abstract.Expression codomain) {
    return cPi((List<Concrete.TypeParameter<Position>>) arguments, (Concrete.Expression) codomain);
  }

  @Nonnull
  @Override
  public Abstract.Expression makeUniverse(@Nullable Abstract.LevelExpression pLevel, @Nullable Abstract.LevelExpression hLevel) {
    return cUniverse((Concrete.LevelExpression) pLevel, (Concrete.LevelExpression) hLevel);
  }

  @Nonnull
  @Override
  public Abstract.LevelExpression makeInferVarLevel(@Nonnull InferenceLevelVariable variable) {
    return new Concrete.InferVarLevelExpression(null, variable);
  }

  @Nonnull
  @Override
  public Abstract.LevelExpression makePLevel() {
    return new Concrete.PLevelExpression(null);
  }

  @Nonnull
  @Override
  public Abstract.LevelExpression makeHLevel() {
    return new Concrete.HLevelExpression(null);
  }

  @Nonnull
  @Override
  public Abstract.LevelExpression makeNumberLevel(int number) {
    return new Concrete.NumberLevelExpression(null, number);
  }

  @Nonnull
  @Override
  public Abstract.LevelExpression makeSucLevel(@Nonnull Abstract.LevelExpression expr) {
    return new Concrete.SucLevelExpression(null, (Concrete.LevelExpression) expr);
  }

  @Nonnull
  @Override
  public Abstract.LevelExpression makeMaxLevel(@Nonnull Abstract.LevelExpression left, @Nonnull Abstract.LevelExpression right) {
    return new Concrete.MaxLevelExpression(null, (Concrete.LevelExpression) left, (Concrete.LevelExpression) right);
  }

  @Nonnull
  @Override
  public Abstract.LevelExpression makeInf() {
    return new Concrete.InfLevelExpression(null);
  }

  @Nonnull
  @Override
  public Abstract.Expression makeGoal(String name, Abstract.Expression expression) {
    return cGoal(name, (Concrete.Expression) expression);
  }

  @Nonnull
  @Override
  public Abstract.Expression makeTuple(@Nonnull List<? extends Abstract.Expression> fields) {
    return cTuple((List<Concrete.Expression<Position>>) fields);
  }

  @Nonnull
  @Override
  public Abstract.Expression makeSigma(@Nonnull List<? extends Abstract.TypeParameter> arguments) {
    return cSigma((List<Concrete.TypeParameter<Position>>) arguments);
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
    return cLet((List<Concrete.LetClause<Position>>) clauses, (Concrete.Expression) expr);
  }

  @Nonnull
  @Override
  public Abstract.LetClause makeLetClause(@Nonnull String name, @Nonnull List<? extends Abstract.Parameter> arguments, @Nonnull Abstract.Expression term) {
    return clet(name, (List<Concrete.Parameter<Position>>) arguments, null, (Concrete.Expression) term);
  }

  @Nonnull
  @Override
  public Abstract.Expression makeCase(@Nonnull List<? extends Abstract.Expression> expressions, @Nonnull List<? extends Abstract.FunctionClause> clauses) {
    return cCase((List<Concrete.Expression<Position>>) expressions, (List<Concrete.FunctionClause<Position>>) clauses);
  }

  @Nonnull
  @Override
  public Abstract.FunctionClause makeClause(@Nonnull List<? extends Abstract.Pattern> patterns, @Nonnull Abstract.Expression expr) {
    return cClause((List<Concrete.Pattern<Position>>) patterns, (Concrete.Expression) expr);
  }

  @Nonnull
  @Override
  public Abstract.Pattern makeConPattern(boolean isExplicit, @Nonnull Abstract.Constructor constructor, @Nonnull List<? extends Abstract.Pattern> args) {
    return cConPattern(isExplicit, constructor.getName(), (List<Concrete.Pattern<Position>>) args);
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
