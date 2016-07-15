package com.jetbrains.jetpad.vclang.term.expr.type;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.PrettyPrintable;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.sort.Sort;
import com.jetbrains.jetpad.vclang.term.expr.sort.SortMaxSet;
import com.jetbrains.jetpad.vclang.term.expr.subst.ExprSubstitution;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.Equations;

import java.util.List;

public interface Type extends PrettyPrintable {
  Type subst(ExprSubstitution substitution);
  Type applyExpressions(List<? extends Expression> expressions);
  boolean isLessOrEquals(Sort sort);
  boolean isLessOrEquals(Expression expression, Equations equations, Abstract.SourceNode sourceNode);
  SortMaxSet toSorts();
}
