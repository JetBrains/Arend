package com.jetbrains.jetpad.vclang.core.context.param;

import com.jetbrains.jetpad.vclang.core.subst.ExprSubstitution;
import com.jetbrains.jetpad.vclang.core.subst.LevelSubstitution;

import java.util.List;

public interface SingleDependentLink extends DependentLink {
  SingleDependentLink getNext();
  TypedSingleDependentLink getNextTyped(List<String> names);
  SingleDependentLink subst(ExprSubstitution exprSubst, LevelSubstitution levelSubst, int size);
}
