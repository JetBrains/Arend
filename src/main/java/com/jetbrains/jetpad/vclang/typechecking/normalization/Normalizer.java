package com.jetbrains.jetpad.vclang.typechecking.normalization;

import com.jetbrains.jetpad.vclang.term.definition.Function;
import com.jetbrains.jetpad.vclang.term.expr.*;

import java.util.EnumSet;
import java.util.List;

public interface Normalizer {
  Expression normalize(LamExpression fun, List<? extends Expression> arguments, List<? extends EnumSet<AppExpression.Flag>> flags);
  Expression normalize(FunCallExpression fun, List<? extends Expression> arguments);
  Expression normalize(ConCallExpression fun, List<? extends Expression> arguments);
  Expression normalize(Function fun, List<? extends Expression> arguments);
  Expression normalize(LetExpression expression);
}
