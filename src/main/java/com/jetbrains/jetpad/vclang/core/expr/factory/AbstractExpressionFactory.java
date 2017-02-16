package com.jetbrains.jetpad.vclang.core.expr.factory;

import com.jetbrains.jetpad.vclang.core.definition.ClassField;
import com.jetbrains.jetpad.vclang.term.Abstract;

import java.util.List;

public interface AbstractExpressionFactory {
  Abstract.Expression makeApp(Abstract.Expression fun, boolean explicit, Abstract.Expression arg);
  Abstract.Expression makeBinOp(Abstract.Expression left, Abstract.Definition defCall, Abstract.Expression right);
  Abstract.Expression makeDefCall(Abstract.Expression expr, Abstract.Definition definition);
  Abstract.Expression makeClassExt(Abstract.Expression expr, List<? extends Abstract.ClassFieldImpl> statements);
  Abstract.ClassFieldImpl makeImplementStatement(ClassField field, Abstract.Expression type, Abstract.Expression term);
  Abstract.Expression makeVar(String name);
  Abstract.Argument makeNameArgument(boolean explicit, String name);
  Abstract.TypeArgument makeTypeArgument(boolean explicit, Abstract.Expression type);
  Abstract.TypeArgument makeTelescopeArgument(boolean explicit, List<String> names, Abstract.Expression type);
  Abstract.Expression makeLam(List<? extends Abstract.Argument> arguments, Abstract.Expression body);
  Abstract.Expression makePi(List<? extends Abstract.TypeArgument> arguments, Abstract.Expression codomain);
  Abstract.Expression makeUniverse(List<? extends Abstract.Expression> plevel, List<? extends Abstract.Expression> hlevel);
  Abstract.Expression makeUniverse(Integer pLevel, Integer hLevel);
  Abstract.Expression makeInferHole();
  Abstract.Expression makeError(Abstract.Expression expr);
  Abstract.Expression makeTuple(List<? extends Abstract.Expression> fields);
  Abstract.Expression makeSigma(List<? extends Abstract.TypeArgument> arguments);
  Abstract.Expression makeProj(Abstract.Expression expr, int field);
  Abstract.Expression makeNew(Abstract.Expression expr);
  Abstract.Expression makeNumericalLiteral(int num);
  Abstract.Expression makeLet(List<? extends Abstract.LetClause> clauses, Abstract.Expression expr);
  Abstract.LetClause makeLetClause(String name, List<? extends Abstract.Argument> arguments, Abstract.Expression resultType, Abstract.Definition.Arrow arrow, Abstract.Expression term);
  Abstract.Expression makeElim(List<? extends Abstract.Expression> exprs, List<? extends Abstract.Clause> clauses);
  Abstract.Expression makeCase(List<? extends Abstract.Expression> expressions, List<? extends Abstract.Clause> clauses);
  Abstract.Clause makeClause(List<? extends Abstract.Pattern> pattern, Abstract.Definition.Arrow arrow, Abstract.Expression expr);
  Abstract.Pattern makeConPattern(String name, List<? extends Abstract.PatternArgument> args);
  Abstract.Pattern makeNamePattern(String name);
  Abstract.PatternArgument makePatternArgument(Abstract.Pattern pattern, boolean explicit);
}
