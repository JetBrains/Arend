package com.jetbrains.jetpad.vclang.typechecking.normalization;

import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.definition.Function;
import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.core.expr.LamExpression;
import com.jetbrains.jetpad.vclang.core.expr.LetExpression;
import com.jetbrains.jetpad.vclang.core.subst.LevelSubstitution;
import com.jetbrains.jetpad.vclang.core.expr.visitor.NormalizeVisitor;

import java.util.List;

public interface Normalizer {
  Expression normalize(LamExpression fun, List<? extends Expression> arguments, NormalizeVisitor.Mode mode);
  Expression normalize(Function fun, LevelSubstitution polySubst, DependentLink params, List<? extends Expression> paramArgs, List<? extends Expression> arguments, NormalizeVisitor.Mode mode);
  Expression normalize(LetExpression expression);
}
