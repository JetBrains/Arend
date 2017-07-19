package com.jetbrains.jetpad.vclang.core.expr.factory;

import com.jetbrains.jetpad.vclang.core.context.binding.inference.InferenceLevelVariable;
import com.jetbrains.jetpad.vclang.core.context.binding.inference.InferenceVariable;
import com.jetbrains.jetpad.vclang.core.definition.ClassField;
import com.jetbrains.jetpad.vclang.term.Abstract;

import java.util.List;

public interface AbstractExpressionFactory {
  Abstract.Expression makeApp(Abstract.Expression fun, boolean explicit, Abstract.Expression arg);
  Abstract.Expression makeBinOp(Abstract.Expression left, Abstract.Definition defCall, Abstract.Expression right);
  Abstract.Expression makeDefCall(Abstract.Expression expr, Abstract.Definition definition);
  Abstract.Expression makeClassExt(Abstract.Expression expr, List<? extends Abstract.ClassFieldImpl> statements);
  Abstract.ClassFieldImpl makeImplementStatement(ClassField field, Abstract.Expression type, Abstract.Expression term);
  Abstract.ReferableSourceNode makeReferable(String name);
  Abstract.Expression makeVar(Abstract.ReferableSourceNode referable);
  Abstract.Expression makeInferVar(InferenceVariable variable);
  Abstract.NameParameter makeNameParameter(boolean explicit, String name);
  Abstract.TypeParameter makeTypeParameter(boolean explicit, Abstract.Expression type);
  Abstract.TypeParameter makeTelescopeParameter(boolean explicit, List<? extends Abstract.ReferableSourceNode> referableList, Abstract.Expression type);
  Abstract.Expression makeLam(List<? extends Abstract.Parameter> arguments, Abstract.Expression body);
  Abstract.Expression makePi(List<? extends Abstract.TypeParameter> arguments, Abstract.Expression codomain);
  Abstract.Expression makeUniverse(Abstract.LevelExpression pLevel, Abstract.LevelExpression hLevel);
  Abstract.LevelExpression makeInferVarLevel(InferenceLevelVariable variable);
  Abstract.LevelExpression makePLevel();
  Abstract.LevelExpression makeHLevel();
  Abstract.LevelExpression makeNumberLevel(int number);
  Abstract.LevelExpression makeSucLevel(Abstract.LevelExpression expr);
  Abstract.LevelExpression makeMaxLevel(Abstract.LevelExpression left, Abstract.LevelExpression right);
  Abstract.LevelExpression makeInf();
  Abstract.Expression makeInferHole();
  Abstract.Expression makeError(Abstract.Expression expr);
  Abstract.Expression makeTuple(List<? extends Abstract.Expression> fields);
  Abstract.Expression makeSigma(List<? extends Abstract.TypeParameter> arguments);
  Abstract.Expression makeProj(Abstract.Expression expr, int field);
  Abstract.Expression makeNew(Abstract.Expression expr);
  Abstract.Expression makeNumericalLiteral(int num);
  Abstract.Expression makeLet(List<? extends Abstract.LetClause> clauses, Abstract.Expression expr);
  Abstract.LetClause makeLetClause(Abstract.ReferableSourceNode referable, List<? extends Abstract.Parameter> arguments, Abstract.Expression resultType, Abstract.Expression term);
  Abstract.Expression makeCase(List<? extends Abstract.Expression> expressions, List<? extends Abstract.FunctionClause> clauses);
  Abstract.FunctionClause makeClause(List<? extends Abstract.Pattern> pattern, Abstract.Expression expr);
  Abstract.Pattern makeConPattern(boolean isExplicit, Abstract.Constructor constructor, List<? extends Abstract.Pattern> args);
  Abstract.Pattern makeNamePattern(boolean isExplicit, String name);
  Abstract.Pattern makeEmptyPattern(boolean isExplicit);
}
