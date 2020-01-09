package org.arend.core.context.param;

import org.arend.core.subst.SubstVisitor;

import javax.annotation.Nonnull;
import java.util.List;

public interface SingleDependentLink extends DependentLink {
  @Nonnull @Override SingleDependentLink getNext();
  @Override TypedSingleDependentLink getNextTyped(List<String> names);
  @Override SingleDependentLink subst(SubstVisitor substVisitor, int size, boolean updateSubst);
}
