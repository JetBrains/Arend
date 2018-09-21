package org.arend.core.context.param;

import org.arend.core.subst.ExprSubstitution;
import org.arend.core.subst.LevelSubstitution;

import java.util.List;

public interface SingleDependentLink extends DependentLink {
  SingleDependentLink getNext();
  TypedSingleDependentLink getNextTyped(List<String> names);
  SingleDependentLink subst(ExprSubstitution exprSubst, LevelSubstitution levelSubst, int size);
}
