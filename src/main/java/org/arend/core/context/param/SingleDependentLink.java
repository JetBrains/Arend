package org.arend.core.context.param;

import org.arend.core.subst.SubstVisitor;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface SingleDependentLink extends DependentLink {
  @NotNull @Override SingleDependentLink getNext();
  @Override TypedSingleDependentLink getNextTyped(List<String> names);
  @Override SingleDependentLink subst(@NotNull SubstVisitor substVisitor, int size, boolean updateSubst);
}
