package com.jetbrains.jetpad.vclang.core.expr.factory;

import com.jetbrains.jetpad.vclang.core.context.binding.inference.InferenceLevelVariable;
import com.jetbrains.jetpad.vclang.core.context.binding.inference.InferenceVariable;
import com.jetbrains.jetpad.vclang.core.definition.ClassField;
import com.jetbrains.jetpad.vclang.term.Abstract;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public interface AbstractExpressionFactory {
  @Nonnull Abstract.Expression makeApp(@Nonnull Abstract.Expression fun, boolean explicit, @Nonnull Abstract.Expression arg);
  @Nonnull Abstract.Expression makeBinOp(@Nonnull Abstract.Expression left, @Nonnull Abstract.Definition defCall, @Nonnull Abstract.Expression right);
  @Nonnull Abstract.Expression makeDefCall(@Nullable Abstract.Expression expr, @Nullable Abstract.Definition definition);
  @Nonnull Abstract.Expression makeClassExt(@Nonnull Abstract.Expression expr, @Nonnull List<? extends Abstract.ClassFieldImpl> statements);
  @Nonnull Abstract.ClassFieldImpl makeImplementStatement(@Nonnull ClassField field, @Nonnull Abstract.Expression term);
  @Nonnull Abstract.ReferableSourceNode makeReferable(@Nullable String name);
  @Nonnull Abstract.Expression makeVar(@Nonnull Abstract.ReferableSourceNode referable);
  @Nonnull Abstract.Expression makeInferVar(@Nonnull InferenceVariable variable);
  @Nonnull Abstract.NameParameter makeNameParameter(boolean explicit, @Nullable String name);
  @Nonnull Abstract.TypeParameter makeTypeParameter(boolean explicit, @Nonnull Abstract.Expression type);
  @Nonnull Abstract.TypeParameter makeTelescopeParameter(boolean explicit, @Nonnull List<? extends Abstract.ReferableSourceNode> referableList, @Nonnull Abstract.Expression type);
  @Nonnull Abstract.Expression makeLam(@Nonnull List<? extends Abstract.Parameter> arguments, @Nonnull Abstract.Expression body);
  @Nonnull Abstract.Expression makePi(@Nonnull List<? extends Abstract.TypeParameter> arguments, @Nonnull Abstract.Expression codomain);
  @Nonnull Abstract.Expression makeUniverse(@Nullable Abstract.LevelExpression pLevel, @Nullable Abstract.LevelExpression hLevel);
  @Nonnull Abstract.LevelExpression makeInferVarLevel(@Nonnull InferenceLevelVariable variable);
  @Nonnull Abstract.LevelExpression makePLevel();
  @Nonnull Abstract.LevelExpression makeHLevel();
  @Nonnull Abstract.LevelExpression makeNumberLevel(int number);
  @Nonnull Abstract.LevelExpression makeSucLevel(@Nonnull Abstract.LevelExpression expr);
  @Nonnull Abstract.LevelExpression makeMaxLevel(@Nonnull Abstract.LevelExpression left, @Nonnull Abstract.LevelExpression right);
  @Nonnull Abstract.LevelExpression makeInf();
  @Nonnull Abstract.Expression makeInferHole();
  @Nonnull Abstract.Expression makeError(@Nullable Abstract.Expression expr);
  @Nonnull Abstract.Expression makeTuple(@Nonnull List<? extends Abstract.Expression> fields);
  @Nonnull Abstract.Expression makeSigma(@Nonnull List<? extends Abstract.TypeParameter> arguments);
  @Nonnull Abstract.Expression makeProj(@Nonnull Abstract.Expression expr, int field);
  @Nonnull Abstract.Expression makeNew(@Nonnull Abstract.Expression expr);
  @Nonnull Abstract.Expression makeNumericalLiteral(int num);
  @Nonnull Abstract.Expression makeLet(@Nonnull List<? extends Abstract.LetClause> clauses, @Nonnull Abstract.Expression expr);
  @Nonnull Abstract.LetClause makeLetClause(@Nonnull String name, @Nonnull List<? extends Abstract.Parameter> arguments, @Nonnull Abstract.Expression term);
  @Nonnull Abstract.Expression makeCase(@Nonnull List<? extends Abstract.Expression> expressions, @Nonnull List<? extends Abstract.FunctionClause> clauses);
  @Nonnull Abstract.FunctionClause makeClause(@Nonnull List<? extends Abstract.Pattern> pattern, @Nonnull Abstract.Expression expr);
  @Nonnull Abstract.Pattern makeConPattern(boolean isExplicit, @Nonnull Abstract.Constructor constructor, @Nonnull List<? extends Abstract.Pattern> args);
  @Nonnull Abstract.Pattern makeNamePattern(boolean isExplicit, @Nullable String name);
  @Nonnull Abstract.Pattern makeEmptyPattern(boolean isExplicit);
}
