package com.jetbrains.jetpad.vclang.term.expr.factory;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.ClassField;
import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.definition.Universe;
import com.jetbrains.jetpad.vclang.term.definition.UniverseOld;

import java.util.List;

public interface AbstractExpressionFactory {
  Abstract.Expression makeApp(Abstract.Expression fun, boolean explicit, Abstract.Expression arg);
  Abstract.Expression makeBinOp(Abstract.Expression left, Definition defCall, Abstract.Expression right);
  Abstract.Expression makeDefCall(Abstract.Expression expr, Definition definition);
  Abstract.Expression makeClassExt(Abstract.Expression expr, List<? extends Abstract.ImplementStatement> statements);
  Abstract.ImplementStatement makeImplementStatement(ClassField field, Abstract.Expression type, Abstract.Expression term);
  Abstract.Expression makeVar(String name);
  Abstract.Argument makeNameArgument(boolean explicit, String name);
  Abstract.TypeArgument makeTypeArgument(boolean explicit, Abstract.Expression type);
  Abstract.TypeArgument makeTelescopeArgument(boolean explicit, List<String> names, Abstract.Expression type);
  Abstract.Expression makeLam(List<? extends Abstract.Argument> arguments, Abstract.Expression body);
  Abstract.Expression makePi(List<? extends Abstract.TypeArgument> arguments, Abstract.Expression codomain);
  Abstract.Expression makeUniverse(Abstract.Expression level);
  Abstract.Expression makeInferHole();
  Abstract.Expression makeError(Abstract.Expression expr);
  Abstract.Expression makeTuple(List<? extends Abstract.Expression> fields);
  Abstract.Expression makeSigma(List<? extends Abstract.TypeArgument> arguments);
  Abstract.Expression makeProj(Abstract.Expression expr, int field);
  Abstract.Expression makeNew(Abstract.Expression expr);
  Abstract.Expression makeLet(List<? extends Abstract.LetClause> clauses, Abstract.Expression expr);
  Abstract.LetClause makeLetClause(String name, List<? extends Abstract.Argument> arguments, Abstract.Expression resultType, Abstract.Definition.Arrow arrow, Abstract.Expression term);
  Abstract.Expression makeElim(List<? extends Abstract.Expression> exprs, List<? extends Abstract.Clause> clauses);
  Abstract.Expression makeCase(List<? extends Abstract.Expression> expressions, List<? extends Abstract.Clause> clauses);
  Abstract.Clause makeClause(List<? extends Abstract.Pattern> pattern, Abstract.Definition.Arrow arrow, Abstract.Expression expr);
  Abstract.Pattern makeConPattern(String name, List<? extends Abstract.PatternArgument> args);
  Abstract.Pattern makeNamePattern(String name);
  Abstract.PatternArgument makePatternArgument(Abstract.Pattern pattern, boolean explicit);
}
