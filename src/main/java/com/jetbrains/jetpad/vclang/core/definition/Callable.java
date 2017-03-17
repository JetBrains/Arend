package com.jetbrains.jetpad.vclang.core.definition;

import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.core.sort.LevelArguments;

import java.util.List;

public interface Callable {
  String getName();
  Expression getTypeWithParams(List<? super DependentLink> params, LevelArguments polyArguments);
  Expression getDefCall(LevelArguments polyArguments, Expression thisExpr, List<Expression> arguments);
}
