package com.jetbrains.jetpad.vclang.term.expr.type;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.PrettyPrintable;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.sort.Sort;
import com.jetbrains.jetpad.vclang.term.expr.sort.SortMax;
import com.jetbrains.jetpad.vclang.term.expr.subst.ExprSubstitution;
import com.jetbrains.jetpad.vclang.term.expr.subst.LevelSubstitution;
import com.jetbrains.jetpad.vclang.term.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.Equations;

import java.util.List;

public interface Type extends PrettyPrintable {
  Type subst(ExprSubstitution exprSubst, LevelSubstitution levelSubst);
  Type applyExpressions(List<? extends Expression> expressions);
  boolean isLessOrEquals(Sort sort);
  boolean isLessOrEquals(Expression expression, Equations equations, Abstract.SourceNode sourceNode);
  SortMax toSorts();
  Type getImplicitParameters(List<DependentLink> params);
  Type fromPiParameters(List<DependentLink> params);
  Type addParameters(DependentLink params);
  DependentLink getParameters();
  Type normalize(NormalizeVisitor.Mode mode);
  Type strip();
  Expression toExpression();
}
