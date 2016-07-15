package com.jetbrains.jetpad.vclang.typechecking.normalization;

import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.definition.Function;
import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.expr.visitor.NormalizeVisitor;

import java.util.EnumSet;
import java.util.List;

public interface Normalizer {
  Expression normalize(LamExpression fun, List<? extends Expression> arguments, List<? extends EnumSet<AppExpression.Flag>> flags, NormalizeVisitor.Mode mode);
  Expression normalize(Function fun, LevelSubstitution polySubst, DependentLink params, List<? extends Expression> paramArgs, List<? extends Expression> arguments, List<? extends Expression> otherArguments, List<? extends EnumSet<AppExpression.Flag>> otherFlags, NormalizeVisitor.Mode mode);
  Expression normalize(LetExpression expression);
}
