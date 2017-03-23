package com.jetbrains.jetpad.vclang.core.expr.type;

import com.jetbrains.jetpad.vclang.core.context.binding.Binding;
import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.core.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.core.sort.Sort;
import com.jetbrains.jetpad.vclang.core.subst.ExprSubstitution;
import com.jetbrains.jetpad.vclang.core.subst.LevelSubstitution;
import com.jetbrains.jetpad.vclang.typechecking.error.LocalErrorReporter;

import java.util.Set;

public interface Type {
  Expression getExpr();
  Sort getSortOfType();
  Type subst(ExprSubstitution exprSubstitution, LevelSubstitution levelSubstitution);
  Type strip(Set<Binding> bounds, LocalErrorReporter errorReporter);
  Type normalize(NormalizeVisitor.Mode mode);
}
