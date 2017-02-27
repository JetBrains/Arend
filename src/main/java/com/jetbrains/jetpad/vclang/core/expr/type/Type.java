package com.jetbrains.jetpad.vclang.core.expr.type;

import com.jetbrains.jetpad.vclang.core.context.binding.Binding;
import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.core.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.core.subst.ExprSubstitution;
import com.jetbrains.jetpad.vclang.core.subst.LevelSubstitution;
import com.jetbrains.jetpad.vclang.typechecking.error.LocalErrorReporter;

import java.util.List;
import java.util.Set;

public interface Type extends TypeMax {
  Type copy();
  Type subst(ExprSubstitution exprSubst, LevelSubstitution levelSubst);
  Type applyExpressions(List<? extends Expression> expressions);
  Type getPiParameters(List<DependentLink> params, boolean normalize, boolean implicitOnly);
  Type fromPiParameters(List<DependentLink> params);
  Type addParameters(DependentLink params, boolean modify);
  Type getPiCodomain();
  Type normalize(NormalizeVisitor.Mode mode);
  Type strip(Set<Binding> bounds, LocalErrorReporter errorReporter);
}
